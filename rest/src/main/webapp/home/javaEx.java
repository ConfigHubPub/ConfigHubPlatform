public static void main(String... args)
{
    // Instantiate ConfigHub API
    ConfigHub configHub =
        new ConfigHub("<token>", "<context>");


    // Request properties
    CHProperties properties = configHub.getProperties();
    int dbPort = properties.getInteger("db.port");
    String dbHost = properties.get("db.host");

    // Request resolved files
    CHFiles files = configHub.getFiles();
    configHub.requestFile("/tomcat/tomee.xml");
}