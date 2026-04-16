package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;

import java.io.*;
import java.util.*;

public class Main {
    private static final Gson gson = new Gson();
    private static WebDriver driver;
    private static final PrintWriter stderr = new PrintWriter(System.err, true);

    private static final List<Tool> tools = Arrays.asList(
        new Tool("launch_browser", "Launch a Chrome browser instance", Map.of()),
        new Tool("navigate_to", "Navigate to a specified URL", Map.of("url", Map.of("type", "string", "description", "The URL to navigate to"))),
        new Tool("find_element", "Find an element by CSS selector", Map.of("selector", Map.of("type", "string", "description", "CSS selector"))),
        new Tool("click_element", "Click on an element", Map.of("selector", Map.of("type", "string", "description", "CSS selector"))),
        new Tool("send_keys", "Send keys to an element", Map.of("selector", Map.of("type", "string", "description", "CSS selector"), "text", Map.of("type", "string", "description", "Text to send"))),
        new Tool("close_browser", "Close the browser", Map.of())
    );

    public static void main(String[] args) {
        // Force line buffering for stdio
        System.setOut(new PrintStream(System.out, true));
        System.setErr(new PrintStream(System.err, true));

        boolean headless = false;
        for (String a : args) {
            if ("--headless".equalsIgnoreCase(a) || "-h".equalsIgnoreCase(a)) {
                headless = true;
                break;
            }
        }

        stderr.println("[MCP Server] Starting Selenium MCP Server (headless=" + headless + ")");

        // Register shutdown hook to cleanup WebDriver when process exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (driver != null) {
                    stderr.println("[MCP Server] Shutdown hook: closing browser");
                    driver.quit();
                    driver = null;
                }
            } catch (Exception e) {
                stderr.println("[MCP Server] Error during shutdown: " + e.getMessage());
            }
        }));

        Scanner scanner = new Scanner(System.in);
        PrintWriter writer = new PrintWriter(System.out, true);

        while (true) {
            try {
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine();

                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                JsonRpcRequest request = gson.fromJson(line, JsonRpcRequest.class);
                JsonRpcResponse response = handleRequest(request);
                String responseJson = gson.toJson(response);
                writer.println(responseJson);
                writer.flush();

            } catch (JsonSyntaxException e) {
                stderr.println("[MCP Server] JSON Parse Error: " + e.getMessage());
                JsonRpcResponse errorResponse = new JsonRpcResponse();
                errorResponse.jsonrpc = "2.0";
                errorResponse.error = Map.of("code", -32700, "message", "Parse error");
                writer.println(gson.toJson(errorResponse));
                writer.flush();
            } catch (Exception e) {
                stderr.println("[MCP Server] Error: " + e.getMessage());
                e.printStackTrace(stderr);
            }
        }

        scanner.close();
        stderr.println("[MCP Server] Shutdown");
    }

    @SuppressWarnings("unchecked")
    private static JsonRpcResponse handleRequest(JsonRpcRequest request) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = request.id;
        response.jsonrpc = "2.0";

        try {
            switch (request.method) {
                case "initialize":
                    response.result = Map.of(
                        "protocolVersion", "2024-11-05",
                        "serverInfo", Map.of(
                            "name", "Selenium MCP Server",
                            "version", "1.0"
                        ),
                        "capabilities", Map.of(
                            "tools", Map.of()
                        )
                    );
                    break;
                case "tools/list":
                    response.result = Map.of("tools", tools);
                    break;
                case "tools/call":
                    Map<String, Object> params = (Map<String, Object>) request.params;
                    String name = (String) params.get("name");
                    Map<String, Object> args = (Map<String, Object>) params.get("arguments");
                    response.result = callTool(name, args != null ? args : new HashMap<>());
                    break;
                default:
                    response.error = Map.of("code", -32601, "message", "Method not found");
            }
        } catch (Exception e) {
            response.error = Map.of("code", -32603, "message", "Internal error: " + e.getMessage());
        }
        return response;
    }

    private static Object callTool(String name, Map<String, Object> args) {
        try {
            switch (name) {
                case "launch_browser":
                    return launchBrowser();
                case "navigate_to":
                    return navigateTo((String) args.get("url"));
                case "find_element":
                    return findElement((String) args.get("selector"));
                case "click_element":
                    return clickElement((String) args.get("selector"));
                case "send_keys":
                    return sendKeys((String) args.get("selector"), (String) args.get("text"));
                case "close_browser":
                    return closeBrowser();
                default:
                    return Map.of("error", "Tool not found");
            }
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private static Object launchBrowser() {
        try {
            if (driver == null) {
                // Ensure driver binary is available
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                // respect HEADLESS env var or system property
                String headlessProp = System.getProperty("mcp.headless");
                String headlessEnv = System.getenv("MCP_HEADLESS");
                boolean headless = "true".equalsIgnoreCase(headlessProp) || "true".equalsIgnoreCase(headlessEnv);
                // Also allow programmatic control via previously-parsed flag saved in system property
                if (headless) {
                    options.addArguments("--headless=new");
                    options.addArguments("--disable-gpu");
                    options.addArguments("--no-sandbox");
                }
                driver = new ChromeDriver(options);
            }
            return Map.of("success", true, "message", "Browser launched successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private static Object navigateTo(String url) {
        if (driver != null) {
            driver.get(url);
            return Map.of("success", true, "message", "Navigated to " + url);
        }
        return Map.of("success", false, "error", "Browser not launched");
    }

    private static Object findElement(String selector) {
        if (driver != null) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                return Map.of("success", true, "tag", element.getTagName(), "text", element.getText());
            } catch (Exception e) {
                return Map.of("success", false, "error", "Element not found: " + e.getMessage());
            }
        }
        return Map.of("success", false, "error", "Browser not launched");
    }

    private static Object clickElement(String selector) {
        if (driver != null) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                element.click();
                return Map.of("success", true, "message", "Clicked element");
            } catch (Exception e) {
                return Map.of("success", false, "error", "Failed to click: " + e.getMessage());
            }
        }
        return Map.of("success", false, "error", "Browser not launched");
    }

    private static Object sendKeys(String selector, String text) {
        if (driver != null) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                element.sendKeys(text);
                return Map.of("success", true, "message", "Sent keys to element");
            } catch (Exception e) {
                return Map.of("success", false, "error", "Failed to send keys: " + e.getMessage());
            }
        }
        return Map.of("success", false, "error", "Browser not launched");
    }

    private static Object closeBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
            return Map.of("success", true, "message", "Browser closed");
        }
        return Map.of("success", false, "error", "Browser not launched");
    }

    @SuppressWarnings("unused")
    static class JsonRpcRequest {
        String jsonrpc;
        String id;
        String method;
        Object params;
    }

    static class JsonRpcResponse {
        String jsonrpc;
        String id;
        Object result;
        Object error;
    }

    static class Tool {
        String name;
        String description;
        Map<String, Object> inputSchema;

        Tool(String name, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }
    }
}
