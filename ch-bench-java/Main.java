import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseFormat;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseProtocol;
import com.clickhouse.client.ClickHouseRecord;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDriver;

public class Main {
    static int useJavaClient(String host, int port, boolean compress, String sql)
            throws InterruptedException, ExecutionException {
        int count = 0;

        ClickHouseNode server = ClickHouseNode.of(host, ClickHouseProtocol.HTTP, port, "system");
        try (ClickHouseClient client = ClickHouseClient.newInstance(server.getProtocol());
                ClickHouseResponse response = client.connect(server).option(ClickHouseClientOption.ASYNC, true)
                        .option(ClickHouseClientOption.COMPRESS, compress)
                        .option(ClickHouseClientOption.FORMAT, ClickHouseFormat.RowBinaryWithNamesAndTypes).query(sql)
                        .execute().get()) {
            for (ClickHouseRecord r : response.records()) {
                count++;
            }
        }

        return count;
    }

    static int useJdbc(String host, int port, boolean compress, String sql) throws SQLException {
        int count = 0;

        String url = new StringBuilder().append("jdbc:ch://").append(host).append(':').append(port).append("/system")
                .toString();
        Properties props = new Properties();
        props.setProperty("compress", String.valueOf(compress));
        try (Connection conn = new ClickHouseDriver().connect(url, props);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                count++;
            }
        }

        return count;
    }

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("dbHost", "localhost");
        int port = Integer.parseInt(System.getProperty("dbPort", "8123"));
        boolean compress = Boolean.parseBoolean(System.getProperty("compress", "true"));
        String sql = "SELECT number FROM system.numbers_mt LIMIT 500000000";

        int count = args != null && args.length > 0 && "client".equals(args[0])
                ? useJavaClient(host, port, compress, sql)
                : useJdbc(host, port, compress, sql);
        // System.out.println(count);
    }
}
