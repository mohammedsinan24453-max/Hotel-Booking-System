import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.sun.net.httpserver.Headers;

public class HotelBookingSystem {
    
    private static final Map<String, RoomInfo> ROOM_INVENTORY = new HashMap<>();
    private static final List<Booking> bookings = new ArrayList<>();
    private static int bookingIdCounter = 1000;
    
    // Hardcoded staff credentials
    private static final String STAFF_USERNAME = "admin";
    private static final String STAFF_PASSWORD = "1234";
    
    static {
        // Initialize room inventory with room counts
        ROOM_INVENTORY.put("Single Room", new RoomInfo(1000, 5));  // 5 single rooms
        ROOM_INVENTORY.put("Double Room", new RoomInfo(1800, 8));  // 8 double rooms
        ROOM_INVENTORY.put("Suite Room", new RoomInfo(3000, 3));   // 3 suite rooms
    }
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/rooms", new RoomsHandler());
        server.createContext("/api/book", new BookingHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/bookings", new BookingsListHandler());
        server.createContext("/api/bookings/delete", new DeleteBookingHandler());
        server.createContext("/api/bookings/update", new UpdateBookingHandler());
        server.createContext("/api/rooms/availability", new RoomAvailabilityHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("🏨 Hotel Booking System Started!");
        System.out.println("📍 Customer Portal: http://localhost:8000/ind.html");
        System.out.println("🔐 Staff Login: http://localhost:8000/staff-login.html");
        System.out.println("👤 Staff Credentials: admin / 1234");
        System.out.println("\nRoom Inventory:");
        ROOM_INVENTORY.forEach((room, info) -> 
            System.out.println("  " + room + ": " + info.totalRooms + " rooms @ ₹" + info.pricePerNight + "/night")
        );
        System.out.println("\nPress Ctrl+C to stop the server.");
    }
    
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            File file = new File("." + path);
            
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                
                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                String response = "404 - File Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".gif")) return "image/gif";
            return "text/plain";
        }
    }
    
    static class RoomsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            
            StringBuilder json = new StringBuilder("[");
            int id = 1;
            
            for (Map.Entry<String, RoomInfo> entry : ROOM_INVENTORY.entrySet()) {
                String roomType = entry.getKey();
                RoomInfo info = entry.getValue();
                String imageName = roomType.toLowerCase().split(" ")[0] + ".jpg";
                
                if (id > 1) json.append(",");
                json.append(String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"price\":%d,\"totalRooms\":%d,\"image\":\"%s\",\"description\":\"%s\"}",
                    id++, roomType, info.pricePerNight, info.totalRooms, imageName, getDescription(roomType)
                ));
            }
            json.append("]");
            
            sendResponse(exchange, 200, json.toString());
        }
        
        private String getDescription(String roomType) {
            switch (roomType) {
                case "Single Room": return "Cozy room for solo travelers";
                case "Double Room": return "Comfortable room for couples";
                case "Suite Room": return "Luxurious suite with premium amenities";
                default: return "Comfortable accommodation";
            }
        }
    }
    
    static class BookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                setCORSHeaders(exchange);
                
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                
                Map<String, String> params = parseQuery(query);
                
                String roomType = params.get("roomType");
                String checkIn = params.get("checkin");
                String checkOut = params.get("checkout");
                
                // Check availability
                if (!isRoomAvailable(roomType, checkIn, checkOut)) {
                    String json = "{\"success\":false,\"message\":\"No rooms available for selected dates\"}";
                    sendResponse(exchange, 200, json);
                    return;
                }
                
                Booking booking = createBookingFromParams(params);
                bookings.add(booking);
                
                String json = bookingToJson(booking);
                sendResponse(exchange, 200, json);
                
                System.out.println("✅ New Booking: " + booking);
            }
        }
    }
    
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                setCORSHeaders(exchange);
                
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                
                Map<String, String> params = parseQuery(query);
                String username = params.get("username");
                String password = params.get("password");
                
                boolean success = STAFF_USERNAME.equals(username) && STAFF_PASSWORD.equals(password);
                
                String json = String.format(
                    "{\"success\":%b,\"message\":\"%s\"}",
                    success,
                    success ? "Login successful" : "Invalid credentials"
                );
                
                sendResponse(exchange, 200, json);
                
                if (success) {
                    System.out.println("🔐 Staff login: " + username);
                }
            }
        }
    }
    
    static class BookingsListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            
            if ("GET".equals(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < bookings.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(bookingToJson(bookings.get(i)));
                }
                json.append("]");
                
                sendResponse(exchange, 200, json.toString());
            }
        }
    }
    
    static class DeleteBookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                setCORSHeaders(exchange);
                
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                
                Map<String, String> params = parseQuery(query);
                int bookingId = Integer.parseInt(params.get("bookingId"));
                
                boolean removed = bookings.removeIf(b -> b.id == bookingId);
                
                String json = String.format(
                    "{\"success\":%b,\"message\":\"%s\"}",
                    removed,
                    removed ? "Booking deleted successfully" : "Booking not found"
                );
                
                sendResponse(exchange, 200, json);
                
                if (removed) {
                    System.out.println("🗑️ Booking deleted: #" + bookingId);
                }
            }
        }
    }
    
    static class UpdateBookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                setCORSHeaders(exchange);
                
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                
                Map<String, String> params = parseQuery(query);
                int bookingId = Integer.parseInt(params.get("bookingId"));
                
                Optional<Booking> bookingOpt = bookings.stream()
                    .filter(b -> b.id == bookingId)
                    .findFirst();
                
                if (bookingOpt.isPresent()) {
                    Booking booking = bookingOpt.get();
                    
                    booking.name = params.get("name");
                    booking.checkIn = params.get("checkin");
                    booking.checkOut = params.get("checkout");
                    booking.guests = params.get("guests");
                    booking.roomType = params.get("roomType");
                    String roomNumberStr = params.get("roomNumber");
                    int roomNumber = Integer.parseInt(roomNumberStr);
                    
                    LocalDate checkInDate = LocalDate.parse(booking.checkIn);
                    LocalDate checkOutDate = LocalDate.parse(booking.checkOut);
                    booking.nights = (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
                    
                    int pricePerNight = ROOM_INVENTORY.get(booking.roomType).pricePerNight;
                    booking.totalPrice = pricePerNight * booking.nights;
                    
                    String json = "{\"success\":true," + bookingToJson(booking).substring(1);
                    sendResponse(exchange, 200, json);
                    
                    System.out.println("✏️ Booking updated: " + booking);
                } else {
                    String json = "{\"success\":false,\"message\":\"Booking not found\"}";
                    sendResponse(exchange, 404, json);
                }
            }
        }
    }
    
    static class RoomAvailabilityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            
            String date = params.get("date");
            if (date == null) {
                date = LocalDate.now().toString();
            }
            
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            
            for (Map.Entry<String, RoomInfo> entry : ROOM_INVENTORY.entrySet()) {
                String roomType = entry.getKey();
                RoomInfo info = entry.getValue();
                
                List<RoomSlot> slots = new ArrayList<>();
                for (int i = 1; i <= info.totalRooms; i++) {
                    boolean isBooked = isRoomBooked(roomType, i, date);
                    String guestName = isBooked ? getGuestName(roomType, i, date) : "";
                    slots.add(new RoomSlot(i, isBooked, guestName));
                }
                
                if (!first) json.append(",");
                first = false;
                
                json.append(String.format(
                    "{\"roomType\":\"%s\",\"totalRooms\":%d,\"price\":%d,\"slots\":%s}",
                    roomType, info.totalRooms, info.pricePerNight, slotsToJson(slots)
                ));
            }
            json.append("]");
            
            sendResponse(exchange, 200, json.toString());
        }
        
        private String slotsToJson(List<RoomSlot> slots) {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < slots.size(); i++) {
                if (i > 0) json.append(",");
                RoomSlot slot = slots.get(i);
                json.append(String.format(
                    "{\"roomNumber\":%d,\"isBooked\":%b,\"guestName\":\"%s\"}",
                    slot.roomNumber, slot.isBooked, slot.guestName
                ));
            }
            json.append("]");
            return json.toString();
        }
    }
    
    // Helper methods
    private static boolean isRoomAvailable(String roomType, String checkIn, String checkOut) {
        RoomInfo info = ROOM_INVENTORY.get(roomType);
        if (info == null) return false;
        
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);
        
        for (int roomNum = 1; roomNum <= info.totalRooms; roomNum++) {
            boolean available = true;
            LocalDate currentDate = checkInDate;
            
            while (!currentDate.isAfter(checkOutDate.minusDays(1))) {
                if (isRoomBooked(roomType, roomNum, currentDate.toString())) {
                    available = false;
                    break;
                }
                currentDate = currentDate.plusDays(1);
            }
            
            if (available) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isRoomBooked(String roomType, int roomNumber, String date) {
        LocalDate targetDate = LocalDate.parse(date);
        
        for (Booking booking : bookings) {
            if (booking.roomType.equals(roomType) && 
                booking.roomNumber == roomNumber) {
                
                LocalDate checkIn = LocalDate.parse(booking.checkIn);
                LocalDate checkOut = LocalDate.parse(booking.checkOut);
                
                if (!targetDate.isBefore(checkIn) && targetDate.isBefore(checkOut)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static String getGuestName(String roomType, int roomNumber, String date) {
        LocalDate targetDate = LocalDate.parse(date);
        
        for (Booking booking : bookings) {
            if (booking.roomType.equals(roomType) && 
                booking.roomNumber == roomNumber) {
                
                LocalDate checkIn = LocalDate.parse(booking.checkIn);
                LocalDate checkOut = LocalDate.parse(booking.checkOut);
                
                if (!targetDate.isBefore(checkIn) && targetDate.isBefore(checkOut)) {
                    return booking.name;
                }
            }
        }
        return "";
    }
    
    private static int findAvailableRoom(String roomType, String checkIn, String checkOut) {
        RoomInfo info = ROOM_INVENTORY.get(roomType);
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);
        
        for (int roomNum = 1; roomNum <= info.totalRooms; roomNum++) {
            boolean available = true;
            LocalDate currentDate = checkInDate;
            
            while (!currentDate.isAfter(checkOutDate.minusDays(1))) {
                if (isRoomBooked(roomType, roomNum, currentDate.toString())) {
                    available = false;
                    break;
                }
                currentDate = currentDate.plusDays(1);
            }
            
            if (available) {
                return roomNum;
            }
        }
        
        return -1;
    }
    
    private static Booking createBookingFromParams(Map<String, String> params) {
        String name = params.get("name");
        String checkIn = params.get("checkin");
        String checkOut = params.get("checkout");
        String guests = params.get("guests");
        String roomType = params.get("roomType");
        
        int roomNumber = findAvailableRoom(roomType, checkIn, checkOut);
        
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        
        int pricePerNight = ROOM_INVENTORY.get(roomType).pricePerNight;
        int totalPrice = (int) (pricePerNight * nights);
        
        return new Booking(
            bookingIdCounter++,
            name,
            checkIn,
            checkOut,
            guests,
            roomType,
            roomNumber,
            (int) nights,
            totalPrice
        );
    }
    
    private static String bookingToJson(Booking booking) {
        return String.format(
            "{\"success\":true,\"bookingId\":%d,\"name\":\"%s\",\"roomType\":\"%s\",\"roomNumber\":%d,\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"guests\":\"%s\",\"nights\":%d,\"totalPrice\":%d}",
            booking.id, booking.name, booking.roomType, booking.roomNumber, booking.checkIn,
            booking.checkOut, booking.guests, booking.nights, booking.totalPrice
        );
    }
    
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    try {
                        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        params.put(key, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return params;
    }
    
    private static void setCORSHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    static class RoomInfo {
        int pricePerNight;
        int totalRooms;
        
        RoomInfo(int pricePerNight, int totalRooms) {
            this.pricePerNight = pricePerNight;
            this.totalRooms = totalRooms;
        }
    }
    
    static class RoomSlot {
        int roomNumber;
        boolean isBooked;
        String guestName;
        
        RoomSlot(int roomNumber, boolean isBooked, String guestName) {
            this.roomNumber = roomNumber;
            this.isBooked = isBooked;
            this.guestName = guestName;
        }
    }
    
    static class Booking {
        int id;
        String name;
        String checkIn;
        String checkOut;
        String guests;
        String roomType;
        int roomNumber;
        int nights;
        int totalPrice;
        
        Booking(int id, String name, String checkIn, String checkOut, String guests,
                String roomType, int roomNumber, int nights, int totalPrice) {
            this.id = id;
            this.name = name;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.guests = guests;
            this.roomType = roomType;
            this.roomNumber = roomNumber;
            this.nights = nights;
            this.totalPrice = totalPrice;
        }
        
        @Override
        public String toString() {
            return String.format("Booking #%d - %s - Room %d (%s) - ₹%d", 
                id, name, roomNumber, roomType, totalPrice);
        }
    }
}