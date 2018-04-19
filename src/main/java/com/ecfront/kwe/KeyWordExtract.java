package com.ecfront.kwe;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class KeyWordExtract {

    private static Map<String, Set<Parser>> RULES = new HashMap<>();
    private static final String LOCAL_RULE_FILE = "kwe-rules.txt";
    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
    private static final ScriptEngine jsEngine = SCRIPT_ENGINE_MANAGER.getEngineByName("nashorn");

    static {
        try {
            loadRules(Helper.readAllByClassPath(LOCAL_RULE_FILE));
        } catch (IOException e) {
            throw new RuntimeException("[KWE]Load local rules error", e);
        }
    }

    public static String extract(String url) {
        try {
            URL targetUrl = new URL(url);
            Set<Parser> parsers = RULES.getOrDefault(targetUrl.getHost(), new HashSet<>());
            for (Parser parser : parsers) {
                Optional<String> matched = parser.parse(targetUrl.getPath(), targetUrl.getQuery());
                if (matched.isPresent()) {
                    return matched.get();
                }
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException("[KWE]Extract url error", e);
        }
    }

    public static long loadOnlineRules(String ruleUrl) throws IOException {
        return loadRules(Helper.httpGet(ruleUrl));
    }

    private static long loadRules(List<String> rules) {
        AtomicLong counter = new AtomicLong();
        rules.forEach(rule -> {
            String[] items = rule.split("\\|");
            if (items.length == 5 || items.length == 2) {
                String host = items[0];
                if (!RULES.containsKey(host)) {
                    RULES.put(host, new HashSet<>());
                }
                RULES.get(host).add(new Parser(items));
                counter.getAndIncrement();
            }
        });
        return counter.get();
    }

    private static class Parser {

        private boolean wdInQuery;
        private int pathIndex;
        private String queryKey;
        private String codec;
        private String enc;
        private String jsFun;

        private Parser(String[] items) {
            if (items.length == 5) {
                wdInQuery = items[1].equalsIgnoreCase("query");
                if (wdInQuery) {
                    queryKey = items[2];
                } else {
                    pathIndex = Integer.valueOf(items[2]);
                }
                codec = items[3];
                enc = items[4];
            } else {
                try {
                    jsFun = items[0].replaceAll("\\.", "_") + "_" + Math.abs(items[1].hashCode());
                    String js = "function " + jsFun + "(uri){\r\n" +
                            "var result = '';\r\n" +
                            items[1] + ";\r\n" +
                            "return result;\r\n" +
                            "}\r\n";
                    jsEngine.eval(js);
                } catch (ScriptException e) {
                    throw new RuntimeException("[KWE]Init JS Function [" + jsFun + "] error", e);
                }
            }
        }

        private Optional<String> parse(String path, String query) {
            if (jsFun == null) {
                if (wdInQuery) {
                    if(query==null||query.equals("")){
                        return Optional.empty();
                    }
                    String[] queryItems = query.split("&");
                    for (String queryItem : queryItems) {
                        if (queryItem.startsWith(queryKey + '=')) {
                            return Optional.of(parse(queryItem.substring(queryKey.length() + 1)));
                        }
                    }
                } else {
                    String[] pathItems = path.split("/");
                    if (pathItems.length > pathIndex) {
                        return Optional.of(parse(pathItems[pathIndex + 1]));
                    }
                }
                return Optional.empty();
            } else {
                try {
                    return Optional.of((String) ((Invocable) jsEngine).invokeFunction(jsFun, path + query));
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
                    default:
                        throw new RuntimeException("Decoder[" + codec + "] NOT Exist.");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("[KWE]Parse decode [" + codec + "] error", e);
            }
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
