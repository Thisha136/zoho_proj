package com.bikebackend.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Single entry point for every VRM API URL. */
public class FrontControllerServlet extends HttpServlet {
    private final Map<String, HttpServlet> handlers = new LinkedHashMap<>();

    @Override
    public void init() throws ServletException {
        register("users", new UserServlet());
        register("bikes", new BikeServlet());
        register("garages", new GarageServlet());
        register("garage-bays", new GarageBayServlet());
        register("garage-services", new GarageServiceServlet());
        register("garage-variants", new GarageVariantServlet());
        register("garage-jobs", new GarageJobServlet());
        register("garage-service-requests", new GarageServiceRequestServlet());
        register("brands", new BrandServlet());
        register("services", new ServiceServlet());
        register("models", new VehicleModelServlet());
        register("variants", new VariantServlet());
        register("service-requests", new ServiceRequestServlet());
        register("invoices", new InvoiceServlet());
        register("payments", new InvoiceServlet());
        register("jobs", new JobServiceServlet());
        register("job-services", new JobServiceServlet());
        register("feedback", new FeedbackServlet());
        register("job-feedback", new FeedbackServlet());
        register("garage-feedback", new FeedbackServlet());
        register("garage-allocations", new BayAllocationServlet());
        register("garage-availability", new BayAllocationServlet());
        register("bike-images", new BikeImageServlet());
        register("uploads", new BikeImageServlet());
        register("address", new AddressServlet());
    }

    private void register(String alias, HttpServlet handler) { handlers.put(alias, handler); }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        if (requestPath.isEmpty() || "/".equals(requestPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "API route not found");
            return;
        }
        String[] parts = splitPath(requestPath);
        Route route = resolve(parts);
        if (route == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "API route not found");
            return;
        }
        for (Map.Entry<String, Long> id : route.ids.entrySet())
            request.setAttribute("vrm.route." + id.getKey() + "Id", id.getValue());
        request.setAttribute("vrm.route.ids", Map.copyOf(route.ids));
        request.setAttribute("vrm.route.resource", parts[0]);
        String originalPrefix = "/" + parts[0];
        String pathInfo = requestPath.length() > originalPrefix.length()
                ? requestPath.substring(originalPrefix.length()) : null;
        route.handler.service(new RoutedRequest(request, "/" + route.alias, pathInfo), response);
    }

    private Route resolve(String[] parts) {
        String root = parts[0];
        String alias = root;
        if ("garages".equals(root) && parts.length > 2) {
            alias = switch (parts[2]) {
                case "bays" -> "garage-bays";
                case "services" -> "garage-services";
                case "variants" -> "garage-variants";
                case "jobs" -> "garage-jobs";
                case "service-requests" -> "garage-service-requests";
                case "bay-allocations" -> "garage-allocations";
                case "bay-availability" -> "garage-availability";
                case "feedback" -> "garage-feedback";
                default -> "garages";
            };
        } else if ("jobs".equals(root) && parts.length > 2 && "feedback".equals(parts[2])) {
            alias = "job-feedback";
        } else if ("invoices".equals(root) && parts.length > 2 && "payments".equals(parts[2])) {
            alias = "invoices";
        }
        HttpServlet handler = handlers.get(alias);
        if (handler == null) return null;
        Map<String, Long> ids = new LinkedHashMap<>();
        if ("users".equals(root)) addId(ids, "uid", parts, 1);
        if ("garages".equals(root)) {
            addId(ids, "gid", parts, 1);
            addIdAfter(ids, "variant_id", parts, "variants");
            addIdAfter(ids, "sid", parts, "services");
            addIdAfter(ids, "bay_id", parts, "bays");
            addIdAfter(ids, "jid", parts, "jobs");
            addIdAfter(ids, "sr_id", parts, "service-requests");
            addIdAfter(ids, "allocation_id", parts, "bay-allocations");
            addIdAfter(ids, "feedback_id", parts, "feedback");
        }
        if ("jobs".equals(root)) {
            addId(ids, "jid", parts, 1);
            addIdAfter(ids, "js_id", parts, "services");
        }
        if ("invoices".equals(root)) addId(ids, "iv_id", parts, 1);
        if ("service-requests".equals(root)) addId(ids, "sr_id", parts, 1);
        if ("bikes".equals(root)) addId(ids, "bike_id", parts, 1);
        if ("brands".equals(root)) addId(ids, "bid", parts, 1);
        if ("models".equals(root)) addId(ids, "model_id", parts, 1);
        if ("variants".equals(root)) addId(ids, "variant_id", parts, 1);
        if ("services".equals(root)) addId(ids, "sid", parts, 1);
        if ("feedback".equals(root)) addId(ids, "feedback_id", parts, 1);
        if ("payments".equals(root)) addId(ids, "payment_id", parts, 1);
        return new Route(alias, handler, ids);
    }

    private static void addId(Map<String, Long> ids, String name, String[] parts, int index) {
        if (index < parts.length) {
            try { ids.put(name, Long.parseLong(parts[index])); }
            catch (NumberFormatException ignored) { }
        }
    }

    private static void addIdAfter(Map<String, Long> ids, String name, String[] parts, String segment) {
        for (int i = 0; i < parts.length - 1; i++)
            if (segment.equals(parts[i])) addId(ids, name, parts, i + 1);
    }

    private static String[] splitPath(String path) {
        return (path.startsWith("/") ? path.substring(1) : path).split("/");
    }

    private record Route(String alias, HttpServlet handler, Map<String, Long> ids) { }

    private static final class RoutedRequest extends HttpServletRequestWrapper {
        private final String servletPath;
        private final String pathInfo;

        private RoutedRequest(HttpServletRequest request, String servletPath, String pathInfo) {
            super(request);
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getPathInfo() {
            return pathInfo;
        }
    }
}
