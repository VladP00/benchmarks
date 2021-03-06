package bench;

import com.dslplatform.json.*;
import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestJava {

    @CompiledJson
    public static class Root {
        public List<Coordinate> coordinates;
    }

    public static class Coordinate {
        public double x, y, z;

        Coordinate(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format("Coordinate {x: %f, y: %f, z: %f}", x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Coordinate coord = (Coordinate) o;
            return x == coord.x && y == coord.y && z == coord.z;
        }
    }

    private static Coordinate calc(byte[] bytes) throws IOException {
        var settings = new DslJson.Settings<Object>()
            .includeServiceLoader()
            .doublePrecision(JsonReader.DoublePrecision.LOW);
        var json = new DslJson<Object>(settings);
        var result = json.deserialize(Root.class, bytes, bytes.length);
        var x = 0.0;
        var y = 0.0;
        var z = 0.0;
        var total = result.coordinates.size();
        for (var m : result.coordinates) {
            x += m.x;
            y += m.y;
            z += m.z;
        }
        return new Coordinate(x / total, y / total, z / total);
    }

    public static void parse(byte[] bytes) throws IOException {
        var start_time = System.currentTimeMillis();
        System.out.println(calc(bytes));
        var time_diff = (System.currentTimeMillis() - start_time) / 1e3;
        System.out.println("time: " + time_diff + " s");
    }

    private static void notify(final String msg) {
        try (var socket = new java.net.Socket("localhost", 9001);
             var out = socket.getOutputStream()) {
            out.write(msg.getBytes("UTF-8"));
        } catch (java.io.IOException e) {
            // standalone usage
        }
    }

    public static void main(String[] args) throws IOException {
        final var right = new Coordinate(1.1, 2.2, 3.3);
        for (final var v : new String[] {
                "{\"coordinates\":[{\"x\":1.1,\"y\":2.2,\"z\":3.3}]}",
                "{\"coordinates\":[{\"y\":2.2,\"x\":1.1,\"z\":3.3}]}"}) {
            final var json = v.getBytes();
            final var left = calc(json);
            if (!left.equals(right)) {
                System.err.printf("%s != %s\n", left, right);
                System.exit(1);
            }
        }

        var bytes = Files.readAllBytes(Paths.get("/tmp/1.json"));

        notify("Java\t" + ProcessHandle.current().pid());

        parse(bytes);

        notify("stop");
    }
}
