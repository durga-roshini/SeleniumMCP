package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;

import java.io.*;
import java.util.*;

public class Main {
    private static final Gson gson = new Gson();
    private static WebDriver driver;
    private static final List<Tool> tools = Arrays.asList(
        new Tool("launch_browser", "Launch a Chrome browser instance", Map.of()),
        new Tool("navigate_to", "Navigate to a specified URL", Map.of("url", Map.of("type", "string", "description", "The URL to navigate to"))),
        new Tool("find_element", "Find an element by CSS selector", Map.of("selector", Map.of("type", "string", "description", "CSS selector"))),
        new Tool("click_element", "Click on an element", Map.of("selector", Map.of("type", "string", "description", "CSS selector"))),
        new Tool("send_keys", "Send keys to an element", Map.of("selector", Map.of("type", "string", "description", "CSS selector"), "text", Map.of("type", "string", "description", "Text to send"))),
        new Tool("close_browser", "Close the browser", Map.of())
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PrintWriter writer = new PrintWriter(System.out);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                JsonRpcRequest request = gson.fromJson(line, JsonRpcRequest.class);
                JsonRpcResponse response = handleRequest(request);
                writer.println(gson.toJson(response));
                writer.flush();
            } catch (Exception e) {
                // Handle error
            }
        }
    }

    private static JsonRpcResponse handleRequest(JsonRpcRequest request) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = request.id;
        response.jsonrpc = "2.0";

        switch (request.method) {
            case "initialize":
                response.result = Map.of("serverInfo", Map.of("name", "Selenium MCP Server", "version", "1.0"));
                break;
            case "tools/list":
                response.result = Map.of("tools", tools);
                break;
            case "tools/call":
                Map<String, Object> params = (Map<String, Object>) request.params;
                String name = (String) params.get("name");
                Map<String, Object> args = (Map<String, Object>) params.get("arguments");
                response.result = callTool(name, args);
                break;
            default:
                response.error = Map.of("code", -32601, "message", "Method not found");
        }
        return response;
    }

    private static Object callTool(String name, Map<String, Object> args) {
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
                return "Tool not found";
        }
    }

    private static Object launchBrowser() {
        if (driver == null) {
            driver = new ChromeDriver();
        }
        return "Browser launched successfully";
    }

    private static Object navigateTo(String url) {
        if (driver != null) {
            driver.get(url);
            return "Navigated to " + url;
        }
        return "Browser not launched";
    }

    private static Object findElement(String selector) {
        if (driver != null) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                return "Element found: " + element.getTagName();
            } catch (Exception e) {
                return "Element not found: " + e.getMessage();
            }
        }
        return "Browser not launched";
    }

    private static Object clickElement(String selector) {
        if (driver != null) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                element.click();
                return "Clicked element";
            } catch (Exception e) {
                return "Failed to click: " + e.getMessage();
            }
        }
        return "Browser not launched";
    }

    private static Object sendKeys(String selector, String text) {
        if (driver != null) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                element.sendKeys(text);
                return "Sent keys to element";
            } catch (Exception e) {
                return "Failed to send keys: " + e.getMessage();
            }
        }
        return "Browser not launched";
    }

    private static Object closeBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
            return "Browser closed";
        }
        return "Browser not launched";
    }

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
