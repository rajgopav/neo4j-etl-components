package org.neo4j.integration.commands;

import java.nio.file.Path;
import java.util.Collection;

import org.neo4j.integration.neo4j.importcsv.ImportFromCsvCommand;
import org.neo4j.integration.neo4j.importcsv.config.Formatting;
import org.neo4j.integration.neo4j.importcsv.config.GraphConfig;
import org.neo4j.integration.neo4j.importcsv.config.ImportConfig;
import org.neo4j.integration.neo4j.importcsv.fields.IdType;
import org.neo4j.integration.sql.ConnectionConfig;
import org.neo4j.integration.sql.DatabaseClient;
import org.neo4j.integration.sql.DatabaseType;
import org.neo4j.integration.sql.exportcsv.ExportToCsvCommand;
import org.neo4j.integration.sql.exportcsv.ExportToCsvConfig;
import org.neo4j.integration.sql.exportcsv.ExportToCsvResults;
import org.neo4j.integration.sql.exportcsv.mysql.MySqlExportService;
import org.neo4j.integration.sql.exportcsv.mysql.schema.JoinMetadataProducer;
import org.neo4j.integration.sql.exportcsv.mysql.schema.TableMetadataProducer;
import org.neo4j.integration.sql.metadata.Join;
import org.neo4j.integration.sql.metadata.Table;
import org.neo4j.integration.sql.metadata.TableName;
import org.neo4j.integration.sql.metadata.TableNamePair;

import static java.lang.String.format;

public class ExportFromMySqlCommand
{
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;
    private final String parentTable;
    private final String childTable;
    private final Environment environment;

    public ExportFromMySqlCommand( String host,
                                   int port,
                                   String user,
                                   String password,
                                   String database,
                                   String parentTable,
                                   String childTable,
                                   Environment environment )
    {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.parentTable = parentTable;
        this.childTable = childTable;
        this.environment = environment;
    }

    public void execute() throws Exception
    {
        Path csvDirectory = environment.prepare();

        print( format( "CSV directory: %s", csvDirectory ) );

        Formatting formatting = Formatting.DEFAULT;

        ConnectionConfig connectionConfig = ConnectionConfig.forDatabase( DatabaseType.MySQL )
                .host( host )
                .port( port )
                .database( database )
                .username( user )
                .password( password )
                .build();

        print( "Exporting from MySQL to CSV..." );

        ExportToCsvResults exportResults = doExport( csvDirectory, formatting, connectionConfig );
        GraphConfig graphConfig = exportResults.createGraphConfig();

        print( "Creating Neo4j store from CSV..." );

        doImport( formatting, graphConfig );

        print( "Done" );
        printResult( environment.destinationDirectory() );
    }

    private void doImport( Formatting formatting, GraphConfig graphConfig ) throws Exception
    {
        ImportConfig importConfig = ImportConfig.builder()
                .importToolDirectory( environment.importToolDirectory() )
                .destination( environment.destinationDirectory() )
                .formatting( formatting )
                .idType( IdType.Integer )
                .graphDataConfig( graphConfig )
                .build();

        new ImportFromCsvCommand( importConfig ).execute();
    }

    private ExportToCsvResults doExport( Path csvDirectory,
                                         Formatting formatting,
                                         ConnectionConfig connectionConfig ) throws Exception
    {
        TableName parent = new TableName( database, parentTable );
        TableName child = new TableName( database, childTable );

        try ( DatabaseClient databaseClient = new DatabaseClient( connectionConfig ) )
        {
            TableMetadataProducer tableMetadataProducer = new TableMetadataProducer( databaseClient );

            Collection<Table> tables1 = tableMetadataProducer.createMetadataFor( parent );
            Collection<Table> tables2 = tableMetadataProducer.createMetadataFor( child );

            Collection<Join> joins =
                    new JoinMetadataProducer( databaseClient ).createMetadataFor( new TableNamePair( parent, child ) );

            ExportToCsvConfig config = ExportToCsvConfig.builder()
                    .destination( csvDirectory )
                    .connectionConfig( connectionConfig )
                    .formatting( formatting )
                    .addTables( tables1 )
                    .addTables( tables2 )
                    .addJoins( joins )
                    .build();

            return new ExportToCsvCommand( config, new MySqlExportService() ).execute();
        }
    }

    private void print( Object message )
    {
        System.err.println( message );
    }

    private void printResult( Object message )
    {
        System.out.println( message );
    }
}
