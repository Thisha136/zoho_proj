package com.bikebackend.servlet;

import com.bikebackend.exception.AppException;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.storage.BikeImageStorage;
import com.bikebackend.model.*;
import com.bikebackend.service.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Front Controller for all /users/* URLs.
 *
 * Supported routes:
 *
 * USER
 * GET    /users
 * POST   /users
 * GET    /users/{uid}
 * PUT    /users/{uid}
 * PATCH  /users/{uid}
 *
 * PASSWORD / STATUS
 * PATCH  /users/{uid}/password
 * PATCH  /users/{uid}/status
 *
 * ADDRESSES
 * GET    /users/{uid}/addresses
 * POST   /users/{uid}/addresses
 * GET    /users/{uid}/addresses/{address_id}
 * PUT    /users/{uid}/addresses/{address_id}
 * PATCH  /users/{uid}/addresses/{address_id}
 * PATCH  /users/{uid}/addresses/{address_id}/default
 * DELETE /users/{uid}/addresses/{address_id}
 *
 * BIKES
 * GET    /users/{uid}/bikes
 * POST   /users/{uid}/bikes
 * GET    /users/{uid}/bikes/{bike_id}
 * PUT    /users/{uid}/bikes/{bike_id}
 * PATCH  /users/{uid}/bikes/{bike_id}
 * DELETE /users/{uid}/bikes/{bike_id}
 *
 * JOBS
 * GET    /users/{uid}/jobs
 * GET    /users/{uid}/jobs/{job_id}
 * GET    /users/{uid}/jobs/count
 */
public class UserServlet extends HttpServlet {

    private final UserService userService = new UserService();
    private final AddressService addressService = new AddressService();
    private final BikeService bikeService = new BikeService();
    private final JobService jobService = new JobService();

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());


    // =========================================================
    // MAIN ROUTER
    // =========================================================

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {

            String method = req.getMethod().toUpperCase();
            String path = req.getPathInfo();

            /*
             * Example:
             *
             * URL:
             * /users/1/bikes/5
             *
             * Because servlet mapping is:
             * /users/*
             *
             * pathInfo becomes:
             * /1/bikes/5
             */

            String[] parts = parseParts(path);

            // =================================================
            // /users
            // =================================================

            if (parts.length == 0) {

                switch (method) {

                    case "GET":
                        handleGetUsers(req, res);
                        break;

                    case "POST":
                        handleCreateUser(req, res);
                        break;

                    default:
                        sendError(
                                res,
                                405,
                                "Method not allowed"
                        );
                }

                return;
            }


            // =================================================
            // /users/{uid}
            // =================================================

            long uid = parseLong(parts[0], "user ID");

            if (parts.length == 1) {

                switch (method) {

                    case "GET":
                        handleGetUser(res, uid);
                        break;

                    case "PUT":
                        handlePutUser(req, res, uid);
                        break;

                    case "PATCH":
                        handlePatchUser(req, res, uid);
                        break;

                    default:
                        sendError(
                                res,
                                405,
                                "Method not allowed"
                        );
                }

                return;
            }


            // =================================================
            // /users/{uid}/{sub-resource}
            // =================================================

            String sub = parts[1];

            switch (sub) {

                case "password":

                    handlePassword(
                            req,
                            res,
                            uid,
                            method,
                            parts
                    );

                    break;


                case "status":

                    handleStatus(
                            req,
                            res,
                            uid,
                            method,
                            parts
                    );

                    break;


                case "addresses":

                    routeAddresses(
                            req,
                            res,
                            uid,
                            method,
                            Arrays.copyOfRange(
                                    parts,
                                    2,
                                    parts.length
                            )
                    );

                    break;


                case "bikes":

                    routeBikes(
                            req,
                            res,
                            uid,
                            method,
                            Arrays.copyOfRange(
                                    parts,
                                    2,
                                    parts.length
                            )
                    );

                    break;


                case "jobs":

                    routeJobs(
                            req,
                            res,
                            uid,
                            method,
                            Arrays.copyOfRange(
                                    parts,
                                    2,
                                    parts.length
                            )
                    );

                    break;


                default:

                    sendError(
                            res,
                            404,
                            "Unknown sub-resource: " + sub
                    );
            }

        }
        catch (AppException e) {

            sendError(
                    res,
                    e.getStatus(),
                    e.getMessage()
            );

        }
        catch (Exception e) {

            handleUnexpected(
                    res,
                    e
            );
        }
    }


    // =========================================================
    // USER CRUD
    // =========================================================

    /**
     * GET /users
     *
     * Optional query parameters:
     *
     * ?search=
     * ?status=
     * ?sort=
     * ?order=
     */
    private void handleGetUsers(
            HttpServletRequest req,
            HttpServletResponse res)
            throws IOException, SQLException {

        String search = req.getParameter("search");
        String status = req.getParameter("status");
        String sort = req.getParameter("sort");
        String order = req.getParameter("order");

        List<User> users =
                userService.getUsers(
                        search,
                        status,
                        sort,
                        order
                );

        List<Map<String, Object>> safeUsers =
                new ArrayList<>();

        for (User user : users) {

            safeUsers.add(
                    safeUser(user)
            );
        }

        write(
                res,
                safeUsers
        );
    }


    /**
     * POST /users
     */
    private void handleCreateUser(
            HttpServletRequest req,
            HttpServletResponse res)
            throws IOException, SQLException {

        User user = readUser(req);

        user.setStatus("ACTIVE");

        long uid =
                userService.createUser(user);

        res.setStatus(
                HttpServletResponse.SC_CREATED
        );

        write(
                res,
                Map.of(
                        "message",
                        "User created successfully",

                        "uid",
                        uid
                )
        );
    }


    /**
     * GET /users/{uid}
     */
    private void handleGetUser(
            HttpServletResponse res,
            long uid)
            throws IOException, SQLException {

        User user =
                userService.getUser(uid);

        write(
                res,
                safeUser(user)
        );
    }


    /**
     * PUT /users/{uid}
     */
    private void handlePutUser(
            HttpServletRequest req,
            HttpServletResponse res,
            long uid)
            throws IOException, SQLException {

        User user =
                readUser(req);

        user.setUid(uid);

        userService.updateUser(user);

        write(
                res,
                Map.of(
                        "message",
                        "User updated successfully"
                )
        );
    }


    /**
     * PATCH /users/{uid}
     */
    private void handlePatchUser(
            HttpServletRequest req,
            HttpServletResponse res,
            long uid)
            throws IOException, SQLException {

        User existing =
                userService.getUser(uid);

        User incoming =
                readUser(req);

        if (incoming.getUsername() != null) {

            existing.setUsername(
                    incoming.getUsername()
            );
        }

        if (incoming.getEmail() != null) {

            existing.setEmail(
                    incoming.getEmail()
            );
        }

        if (incoming.getPhNo() != null) {

            existing.setPhNo(
                    incoming.getPhNo()
            );
        }

        existing.setUid(uid);

        userService.updateUser(existing);

        write(
                res,
                Map.of(
                        "message",
                        "User partially updated successfully"
                )
        );
    }


    // =========================================================
    // PASSWORD
    // =========================================================

    /**
     * PATCH /users/{uid}/password
     */
    private void handlePassword(
            HttpServletRequest req,
            HttpServletResponse res,
            long uid,
            String method,
            String[] parts)
            throws IOException, SQLException {

        if (parts.length != 2) {

            sendError(
                    res,
                    404,
                    "Invalid password URL"
            );

            return;
        }

        if (!"PATCH".equals(method)) {

            sendError(
                    res,
                    405,
                    "Method not allowed"
            );

            return;
        }

        JsonNode body =
                objectMapper.readTree(
                        req.getReader()
                );

        String password =
                body.has("password")
                        ? body.get("password").asText(null)
                        : null;

        userService.changePassword(
                uid,
                password
        );

        write(
                res,
                Map.of(
                        "message",
                        "Password changed successfully"
                )
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    /**
     * PATCH /users/{uid}/status
     */
    private void handleStatus(
            HttpServletRequest req,
            HttpServletResponse res,
            long uid,
            String method,
            String[] parts)
            throws IOException, SQLException {

        if (parts.length != 2) {

            sendError(
                    res,
                    404,
                    "Invalid status URL"
            );

            return;
        }

        if (!"PATCH".equals(method)) {

            sendError(
                    res,
                    405,
                    "Method not allowed"
            );

            return;
        }

        JsonNode body =
                objectMapper.readTree(
                        req.getReader()
                );

        String status =
                body.has("status")
                        ? body.get("status").asText(null)
                        : null;

        userService.changeStatus(
                uid,
                status
        );

        write(
                res,
                Map.of(
                        "message",
                        "User status updated successfully"
                )
        );
    }


    // =========================================================
    // ADDRESSES
    // =========================================================

    /**
     * /users/{uid}/addresses
     * /users/{uid}/addresses/{address_id}
     * */
     private void routeAddresses(
     HttpServletRequest req,
     HttpServletResponse res,
     long uid,
     String method,
     String[] rest)
     throws IOException, SQLException {

     if (rest.length > 2) {
     sendError(res, 404, "Invalid address URL");
     return;
     }

     Long addressId = null;

     if (rest.length >= 1) {
     addressId = parseLongOrNull(rest[0]);

     if (addressId == null) {
     sendError(res, 400, "Invalid address ID");
     return;
     }
     }

     String action = rest.length == 2 ? rest[1] : null;

     switch (method) {

     case "GET": {
     if (action != null) {
     sendError(res, 404, "Invalid address URL");
     return;
     }

     if (addressId != null) {
     write(res, addressService.getAddress(uid, addressId));
     } else {
     write(res, addressService.getAddresses(uid));
     }

     break;
     }

     case "POST": {
     if (addressId != null || action != null) {
     sendError(res, 400,
     "POST /addresses does not accept an ID");
     return;
     }

     Address address = readAddress(req);
     address.setUid(uid);

     long createdAddressId =
     addressService.createAddress(address);

     res.setStatus(HttpServletResponse.SC_CREATED);

     write(res, Map.of(
     "message", "Address created successfully",
     "address_id", createdAddressId
     ));

     break;
     }

     case "PUT": {
     if (addressId == null || action != null) {
     sendError(res, 400, "Address ID is required");
     return;
     }

     Address address = readAddress(req);
     address.setAddressId(addressId);

     addressService.updateAddress(uid, address);

     write(res, Map.of(
     "message", "Address updated successfully"
     ));

     break;
     }

     case "PATCH": {
     if (addressId == null) {
     sendError(res, 400, "Address ID is required");
     return;
     }

     if (action != null && !"default".equals(action)) {
     sendError(res, 404, "Unknown address action");
     return;
     }

     if ("default".equals(action)) {

     addressService.setDefault(uid, addressId);

     write(res, Map.of(
     "message", "Default address updated"
     ));

     } else {

     Address address = readAddress(req);

     addressService.partialUpdateAddress(
     uid,
     addressId,
     address
     );

     write(res, Map.of(
     "message",
     "Address partially updated"
     ));
     }

     break;
     }

     case "DELETE": {
     if (addressId == null || action != null) {
     sendError(res, 400, "Address ID is required");
     return;
     }

     addressService.deleteAddress(uid, addressId);

     write(res, Map.of(
     "message", "Address deleted successfully"
     ));

     break;
     }

     default:
     sendError(res, 405, "Method not allowed");
     }
     }


    // =========================================================
    // BIKES
    // =========================================================

    /**
     * /users/{uid}/bikes
     * /users/{uid}/bikes/{bike_id}
     *
     * IMPORTANT:
     *
     * There is NO:
     *
     *
     * Bike requests come here through:
     *
     */
    private void routeBikes(
            HttpServletRequest req,
            HttpServletResponse res,
            long uid,
            String method,
            String[] rest)
            throws IOException, SQLException, ServletException {

        /*
         * /users/1/bikes
         *
         * rest = []
         *
         * /users/1/bikes/5
         *
         * rest = ["5"]
         */

        if (rest.length > 1) {

            sendError(
                    res,
                    404,
                    "Invalid bike URL"
            );

            return;
        }

        Long bikeId = null;

        if (rest.length == 1) {

            bikeId =
                    parseLongOrNull(rest[0]);

            if (bikeId == null) {

                sendError(
                        res,
                        400,
                        "Invalid bike ID"
                );

                return;
            }
        }


        switch (method) {

            case "GET": {

                if (bikeId != null) {

                    // GET /users/{uid}/bikes/{bike_id}

                    write(
                            res,
                            bikeService.getBike(
                                    uid,
                                    bikeId
                            )
                    );

                } else {

                    // GET /users/{uid}/bikes

                    write(
                            res,
                            bikeService.getBikes(
                                    uid
                            )
                    );
                }

                break;
            }

            case "POST": {
                if (bikeId != null) {

                    sendError(
                            res,
                            400,
                            "POST /bikes does not accept bike ID"
                    );

                    return;
                }

                // POST /users/{uid}/bikes

                Bike bike = isMultipart(req)
                        ? readMultipartBike(req)
                        : readBike(req);

                // Important:
                // UID comes from URL, not JSON.

                bike.setUid(uid);

                long createdBikeId =
                        bikeService.createBike(
                                bike
                        );

                res.setStatus(
                        HttpServletResponse.SC_CREATED
                );

                write(
                        res,
                        Map.of(
                                "message",
                                "Bike created successfully",

                                "bike_id",
                                createdBikeId
                        )
                );

                break;

            }
            case "PUT": {
                if (bikeId == null) {

                    sendError(
                            res,
                            400,
                            "Bike ID is required"
                    );

                    return;
                }

                // PUT /users/{uid}/bikes/{bike_id}

                Bike bike =
                        readBike(req);

                bike.setBikeId(
                        bikeId
                );

                bikeService.updateBike(
                        uid,
                        bike
                );

                write(
                        res,
                        Map.of(
                                "message",
                                "Bike updated successfully"
                        )
                );

                break;

            }
            case "PATCH": {
                if (bikeId == null) {

                    sendError(
                            res,
                            400,
                            "Bike ID is required"
                    );

                    return;
                }

                // PATCH /users/{uid}/bikes/{bike_id}

                Bike bike =
                        readBike(req);

                bikeService.partialUpdateBike(
                        uid,
                        bikeId,
                        bike
                );

                write(
                        res,
                        Map.of(
                                "message",
                                "Bike partially updated"
                        )
                );

                break;

            }
            case "DELETE": {
                if (bikeId == null) {

                    sendError(
                            res,
                            400,
                            "Bike ID is required"
                    );

                    return;
                }

                // DELETE /users/{uid}/bikes/{bike_id}

                bikeService.deleteBike(
                        uid,
                        bikeId
                );

                write(
                        res,
                        Map.of(
                                "message",
                                "Bike deleted successfully"
                        )
                );

                break;
            }

            default:
            {
                sendError(
                        res,
                        405,
                        "Method not allowed"
                );}
        }
    }


    // =========================================================
    // JOBS
    // =========================================================

    /**
     * GET /users/{uid}/jobs
     * GET /users/{uid}/jobs/{jid}
     * GET /users/{uid}/jobs/count
     */
    private void routeJobs(
            HttpServletRequest req,
            HttpServletResponse res,
            long uid,
            String method,
            String[] rest)
            throws IOException, SQLException {

        if (!"GET".equals(method)) {

            sendError(
                    res,
                    405,
                    "Method not allowed"
            );

            return;
        }

        if (rest.length > 1) {

            sendError(
                    res,
                    404,
                    "Invalid job URL"
            );

            return;
        }

        String segment =
                rest.length == 1
                        ? rest[0]
                        : null;


        // /users/{uid}/jobs/count

        if ("count".equals(segment)) {

            String status =
                    req.getParameter("status");

            write(
                    res,
                    Map.of(
                            "count",
                            jobService.countJobs(
                                    null,
                                    uid,
                                    status
                            )
                    )
            );

            return;
        }


        // /users/{uid}/jobs/{jid}

        if (segment != null) {

            long jid =
                    parseLong(
                            segment,
                            "job ID"
                    );

            write(
                    res,
                    jobService.getUserJob(
                            uid,
                            jid
                    )
            );

            return;
        }


        // /users/{uid}/jobs

        write(
                res,
                jobService.getUserJobs(
                        uid
                )
        );
    }


    // =========================================================
    // JSON READERS
    // =========================================================

    private User readUser(
            HttpServletRequest req)
            throws IOException {

        JsonNode json =
                objectMapper.readTree(
                        req.getReader()
                );

        User user =
                new User();

        if (json.has("username") &&
                !json.get("username").isNull()) {

            user.setUsername(
                    json.get("username").asText()
            );
        }

        if (json.has("email") &&
                !json.get("email").isNull()) {

            user.setEmail(
                    json.get("email").asText()
            );
        }

        if (json.has("password") &&
                !json.get("password").isNull()) {

            user.setPassword(
                    json.get("password").asText()
            );
        }

        if (json.has("ph_no") &&
                !json.get("ph_no").isNull()) {

            user.setPhNo(
                    json.get("ph_no").asText()
            );
        }

        if (json.has("status") &&
                !json.get("status").isNull()) {

            user.setStatus(
                    json.get("status").asText()
            );
        }

        return user;
    }


    private Address readAddress(
            HttpServletRequest req)
            throws IOException {

        JsonNode json =
                objectMapper.readTree(
                        req.getReader()
                );

        Address address =
                new Address();

        if (json.has("address_type") &&
                !json.get("address_type").isNull()) {

            address.setAddressType(
                    json.get("address_type").asText()
            );
        }

        if (json.has("address_line") &&
                !json.get("address_line").isNull()) {

            address.setAddressLine(
                    json.get("address_line").asText()
            );
        }

        if (json.has("city") &&
                !json.get("city").isNull()) {

            address.setCity(
                    json.get("city").asText()
            );
        }

        if (json.has("state") &&
                !json.get("state").isNull()) {

            address.setState(
                    json.get("state").asText()
            );
        }

        if (json.has("pincode") &&
                !json.get("pincode").isNull()) {

            address.setPincode(
                    json.get("pincode").asText()
            );
        }

        if (json.has("is_default") &&
                !json.get("is_default").isNull()) {

            address.setIsDefault(
                    json.get("is_default").asBoolean()
            );
        }

        return address;
    }


    private Bike readBike(
            HttpServletRequest req)
            throws IOException {

        JsonNode json =
                objectMapper.readTree(
                        req.getReader()
                );

        Bike bike =
                new Bike();

        if (json.has("variant_id") &&
                !json.get("variant_id").isNull()) {

            bike.setVariantId(
                    json.get("variant_id").asLong()
            );
        }

        if (json.has("manufacturing_year") &&
                !json.get("manufacturing_year").isNull()) {

            bike.setManufacturingYear(
                    json.get("manufacturing_year").asInt()
            );
        }

        if (json.has("purchase_date") &&
                !json.get("purchase_date").isNull()) {

            bike.setPurchaseDate(
                    LocalDate.parse(
                            json.get("purchase_date").asText()
                    )
            );
        }

        if (json.has("registration_no") &&
                !json.get("registration_no").isNull()) {

            bike.setRegistrationNo(
                    json.get("registration_no").asText()
            );
        }

        if (json.has("fuel_type") &&
                !json.get("fuel_type").isNull()) {

            bike.setFuelType(
                    json.get("fuel_type").asText()
            );
        }

        if (json.has("odometer_reading") &&
                !json.get("odometer_reading").isNull()) {

            bike.setOdometerReading(
                    json.get("odometer_reading").asInt()
            );
        }

        if (json.has("color") &&
                !json.get("color").isNull()) {

            bike.setColor(
                    json.get("color").asText()
            );
        }

        if (json.has("image_url") &&
                !json.get("image_url").isNull()) {
            bike.setImageUrl(json.get("image_url").asText());
        }

        return bike;
    }

    private boolean isMultipart(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }

    private Bike readMultipartBike(HttpServletRequest req) throws IOException, ServletException {
        Bike bike = new Bike();
        bike.setVariantId(formLong(req, "variant_id"));
        bike.setManufacturingYear(formInteger(req, "manufacturing_year"));
        String purchaseDate = formText(req, "purchase_date");
        if (purchaseDate != null && !purchaseDate.isBlank()) bike.setPurchaseDate(LocalDate.parse(purchaseDate));
        bike.setRegistrationNo(formText(req, "registration_no"));
        bike.setFuelType(formText(req, "fuel_type"));
        bike.setOdometerReading(formInteger(req, "odometer_reading"));
        bike.setColor(formText(req, "color"));

        jakarta.servlet.http.Part image = req.getPart("image");
        if (image != null && image.getSize() > 0) {
            bike.setImageUrl(BikeImageStorage.store(req, image));
        }
        return bike;
    }

    private String formText(HttpServletRequest req, String name) throws IOException, ServletException {
        jakarta.servlet.http.Part part = req.getPart(name);
        return part == null ? null : new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private Integer formInteger(HttpServletRequest req, String name) throws IOException, ServletException {
        String value = formText(req, name);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private Long formLong(HttpServletRequest req, String name) throws IOException, ServletException {
        String value = formText(req, name);
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private String extensionFor(String contentType, String submittedName) {
        if (submittedName != null && submittedName.contains("."))
            return submittedName.substring(submittedName.lastIndexOf('.')).toLowerCase();
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }


    // =========================================================
    // PATH HELPERS
    // =========================================================

    /**
     * Converts:
     *
     * /1/bikes/5
     *
     * into:
     *
     * ["1", "bikes", "5"]
     */
    private String[] parseParts(
            String pathInfo) {

        if (pathInfo == null ||
                pathInfo.equals("/") ||
                pathInfo.isEmpty()) {

            return new String[0];
        }

        String trimmed =
                pathInfo.startsWith("/")
                        ? pathInfo.substring(1)
                        : pathInfo;

        if (trimmed.isEmpty()) {

            return new String[0];
        }

        return trimmed.split("/");
    }


    private long parseLong(
            String value,
            String label) {

        try {

            return Long.parseLong(value);

        } catch (NumberFormatException e) {

            throw new BadRequestException(
                    "Invalid " + label
            );
        }
    }


    private Long parseLongOrNull(
            String value) {

        try {

            return Long.parseLong(value);

        } catch (NumberFormatException e) {

            return null;
        }
    }


    // =========================================================
    // SAFE USER RESPONSE
    // =========================================================

    /**
     * Never return password in API response.
     */
    private Map<String, Object> safeUser(
            User user) {

        Map<String, Object> map =
                new HashMap<>();

        map.put(
                "uid",
                user.getUid()
        );

        map.put(
                "username",
                user.getUsername()
        );

        map.put(
                "email",
                user.getEmail()
        );

        map.put(
                "ph_no",
                user.getPhNo()
        );

        map.put(
                "status",
                user.getStatus()
        );

        map.put(
                "created_at",
                user.getCreatedAt()
        );

        map.put(
                "updated_at",
                user.getUpdatedAt()
        );

        map.put(
                "version",
                user.getVersion()
        );

        return map;
    }


    // =========================================================
    // RESPONSE HELPERS
    // =========================================================

    private void write(
            HttpServletResponse res,
            Object body)
            throws IOException {

        objectMapper.writeValue(
                res.getWriter(),
                body
        );
    }


    private void sendError(
            HttpServletResponse res,
            int status,
            String message)
            throws IOException {

        res.setStatus(status);

        res.setContentType(
                "application/json"
        );

        res.setCharacterEncoding(
                "UTF-8"
        );

        objectMapper.writeValue(
                res.getWriter(),
                Map.of(
                        "error",
                        message
                )
        );
    }


    private void handleUnexpected(
            HttpServletResponse res,
            Exception e)
            throws IOException {

        e.printStackTrace();

        sendError(
                res,
                500,
                "Internal server error"
        );
    }
}
