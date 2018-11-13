package com.ecfront.kwe;

import javax.script.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class KeyWordExtract {

    private static Map<String, Set<Parser>> RULES = new HashMap<>();
    private static final String LOCAL_RULE_FILE = "kwe-rules.txt";
    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
    private static Invocable invocable;

    static {
        try {
            loadRules(Helper.readAllByClassPath(LOCAL_RULE_FILE));
        } catch (IOException e) {
            throw new RuntimeException("[KWE]Load local rules error", e);
        }
    }

    public static Result extract(String uri) {
        try {
            URI u = null;
            try {
                u = new URI(uri);
            } catch (URISyntaxException e) {
                if (e.getReason().contains("Malformed escape pair")) {
                    uri = uri.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                    u = new URI(uri);
                }
            }
            assert u != null;
            return extract(u);
        } catch (Exception e) {
            throw new RuntimeException("[KWE]Extract uri [" + uri + "] error", e);
        }
    }

    public static Result extract(URI uri) {
        if (!RULES.containsKey(uri.getHost())) {
            return null;
        }
        Set<Parser> parsers = RULES.get(uri.getHost().toLowerCase());
        for (Parser parser : parsers) {
            Optional<Result> matched = parser.parse(uri.getRawPath(), uri.getRawQuery());
            if (matched.isPresent()) {
                return matched.get();
            }
        }
        return null;
    }

    public static long loadOnlineRules(String ruleUrl) throws IOException {
        return loadRules(Helper.httpGet(ruleUrl));
    }

    private static long loadRules(List<String> rules) {
        AtomicLong counter = new AtomicLong();
        StringBuffer sb = new StringBuffer();
        rules.forEach(rule -> {
            String[] items = rule.split("\\|", -1);
            if (items.length == 6 || items.length == 3) {
                String host = items[1].toLowerCase();
                if (!RULES.containsKey(host)) {
                    RULES.put(host, new HashSet<>());
                }
                Parser parser = new Parser(items);
                RULES.get(host).add(parser);
                counter.getAndIncrement();
                if (parser.jsStr != null) {
                    sb.append(parser.jsStr + "\r\n");
                }
            }
        });
        try {
            Compilable jsEngine = (Compilable) SCRIPT_ENGINE_MANAGER.getEngineByName("nashorn");
            CompiledScript script = jsEngine.compile(sb.toString());
            script.eval();
            invocable = (Invocable) script.getEngine();
        } catch (ScriptException e) {
            throw new RuntimeException("[KWE]Init JS Function error", e);
        }
        return counter.get();
    }

    private static class Parser {

        private String name;
        private boolean wdInQuery;
        private int pathIndex;
        private String queryKey;
        private String codec;
        private String enc;
        private String jsFun;
        private String jsStr;

        private Parser(String[] items) {
            name = items[0];
            if (items.length == 6) {
                wdInQuery = items[2].equalsIgnoreCase("query");
                if (wdInQuery) {
                    queryKey = items[3].toLowerCase();
                } else {
                    pathIndex = Integer.valueOf(items[3]);
                }
                codec = items[4];
                enc = items[5];
            } else {
                jsFun = items[1].replaceAll("\\.", "_") + "_" + Math.abs(items[2].hashCode());
                jsStr = "function " + jsFun + "(uri){\r\n" +
                        "var result = '';\r\n" +
                        items[2] + ";\r\n" +
                        "return result;\r\n" +
                        "}\r\n";
            }
        }

        private Optional<Result> parse(String path, String query) {
            if (jsFun == null) {
                if (wdInQuery) {
                    if (query == null || query.equals("")) {
                        return Optional.empty();
                    }
                    String[] queryItems = query.split("&");
                    for (String queryItem : queryItems) {
                        if (queryItem.toLowerCase().startsWith(queryKey + '=')) {
                            String keyVal = parse(queryItem.substring(queryKey.length() + 1));
                            return Optional.of(new Result(name, keyVal));
                        }
                    }
                } else {
                    String[] pathItems = path.split("/");
                    if (pathItems.length > pathIndex) {
                        String keyVal = parse(pathItems[pathIndex + 1]);
                        return Optional.of(new Result(name, keyVal));
                    }
                }
                return Optional.empty();
            } else {
                try {
                    String keyVal = (String) invocable.invokeFunction(jsFun, path + query);
                    return Optional.of(new Result(name, keyVal));
                } catch (ScriptException | NoSuchMethodException e) {
                    throw new RuntimeException("[KWE]Execute JS Function [" + jsFun + "] error", e);
                }
            }
        }

        private String parse(String encodeValue) {
            try {
                switch (codec.toLowerCase()) {
                    case "decodeuri":
                        return URLDecoder.decode(encodeValue, enc);
                    case "":
                        return encodeValue;
                    default:
                        throw new RuntimeException("Decoder[" + codec + "] NOT Exist.");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("[KWE]Parse decode [" + codec + "] error", e);
            }
        }

    }

    public static class Result {

        public String name;
        public String value;

        public Result(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private static class Helper {

        private static List<String> readAllByClassPath(String classpath) throws IOException {
            File file = new File(KeyWordExtract.class.getResource("/").getPath() + classpath);
            if (file.exists()) {
                return Files.readAllLines(file.toPath());
            }
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpath);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
            return buffer.lines().collect(Collectors.toList());
        }

        private static List<String> httpGet(String url) throws IOException {
            URL getUrl = new URL(url);
            URLConnection connection = getUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return buffer.lines().collect(Collectors.toList());
        }

    }


}
