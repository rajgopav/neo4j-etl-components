package org.neo4j.integration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.neo4j.integration.mysql.MySqlClient;
import org.neo4j.integration.neo4j.Neo4j;
import org.neo4j.integration.provisioning.Neo4jFixture;
import org.neo4j.integration.provisioning.Server;
import org.neo4j.integration.provisioning.ServerFixture;
import org.neo4j.integration.provisioning.scripts.MySqlScripts;
import org.neo4j.integration.sql.DatabaseType;
import org.neo4j.integration.util.ResourceRule;
import org.neo4j.integration.util.TemporaryDirectory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import static org.neo4j.integration.neo4j.Neo4j.NEO4J_VERSION;
import static org.neo4j.integration.provisioning.platforms.TestType.INTEGRATION;

public class GenerateMetadataMappingIntegrationTest
{
    @ClassRule
    public static final ResourceRule<Path> tempDirectory =
            new ResourceRule<>( TemporaryDirectory.temporaryDirectory() );

    @ClassRule
    public static final ResourceRule<Server> mySqlServer = new ResourceRule<>(
            ServerFixture.server(
                    "mysql-integration-test-create-csv",
                    DatabaseType.MySQL.defaultPort(),
                    MySqlScripts.startupScript(),
                    tempDirectory.get(), INTEGRATION ) );

    @ClassRule
    public static final ResourceRule<Neo4j> neo4j =
            new ResourceRule<>( Neo4jFixture.neo4j( NEO4J_VERSION, tempDirectory.get() ) );

    @BeforeClass
    public static void setUp() throws Exception
    {
        MySqlClient client = new MySqlClient( mySqlServer.get().ipAddress() );
        client.execute( MySqlScripts.setupDatabaseScript().value() );
    }

    @Test
    public void shouldGenerateMappingsFileSuccessfully() throws Exception
    {
        String mapping = createMetadataMappings( "javabase" );
        assertThat( mapping, containsString( "STUDENT_ID" ) );
    }

    private static String createMetadataMappings( String database ) throws IOException
    {
        Path importToolOptions = tempDirectory.get().resolve( "import-tool-options.json" );
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<Object, Object> options = new HashMap<>();
        options.put( "delimiter", "\t" );
        options.put( "quote", "`" );
        options.put( "multiline-fields", "true" );
        objectMapper.writeValue( importToolOptions.toFile(), options );

        return NeoIntegrationCli.executeMainReturnSysOut( new String[]{
                "mysql",
                "generate-metadata-mapping",
                "--host", mySqlServer.get().ipAddress(),
                "--user", MySqlClient.Parameters.DBUser.value(),
                "--password", MySqlClient.Parameters.DBPassword.value(),
                "--database", database,
                "--options-file", importToolOptions.toString(),
                "--relationship-name", "column",
                "--debug"} );
    }
}
