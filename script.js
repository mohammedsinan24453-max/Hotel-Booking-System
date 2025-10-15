// Global state
let selectedRoom = null;

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    loadRooms();
    setupFormValidation();
});

// Load rooms from backend
async function loadRooms() {
    try {
        const response = await fetch('http://localhost:8000/api/rooms');
        const rooms = await response.json();
        displayRooms(rooms);
    } catch (error) {
        console.error('Error loading rooms:', error);
        document.getElementById('roomsGrid').innerHTML = 
            '<p style="color: red;">Failed to load rooms. Please make sure the server is running.</p>';
    }
}

// Display rooms on homepage
function displayRooms(rooms) {
    const grid = document.getElementById('roomsGrid');
    grid.innerHTML = '';
    
    rooms.forEach(room => {
        const card = document.createElement('div');
        card.className = 'room-card';
        card.innerHTML = `
            <img src="${room.image}" alt="${room.name}">
            <div class="room-info">
                <h3>${room.name}</h3>
                <p>${room.description}</p>
                <div class="room-price">₹${room.price}/night</div>
                <button class="book-btn" onclick="bookRoom('${room.name}', ${room.price})">
                    Book Now
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

// Show booking form
function bookRoom(roomName, price) {
    selectedRoom = { name: roomName, price: price };
    document.getElementById('homePage').style.display = 'none';
    document.getElementById('bookingPage').style.display = 'block';
    document.getElementById('confirmationPage').style.display = 'none';
    
    // Pre-select room type
    document.getElementById('roomType').value = roomName;
    
    // Set minimum date to today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('checkin').min = today;
    document.getElementById('checkout').min = today;
}

// Show home page
function showHome() {
    document.getElementById('homePage').style.display = 'block';
    document.getElementById('bookingPage').style.display = 'none';
    document.getElementById('confirmationPage').style.display = 'none';
    document.getElementById('bookingForm').reset();
}

// Setup form validation
function setupFormValidation() {
    const form = document.getElementById('bookingForm');
    const checkinInput = document.getElementById('checkin');
    const checkoutInput = document.getElementById('checkout');
    
    // Update checkout min date when checkin changes
    checkinInput.addEventListener('change', () => {
        const checkinDate = new Date(checkinInput.value);
        checkinDate.setDate(checkinDate.getDate() + 1);
        checkoutInput.min = checkinDate.toISOString().split('T')[0];
        
        if (checkoutInput.value && new Date(checkoutInput.value) <= new Date(checkinInput.value)) {
            checkoutInput.value = '';
        }
    });
    
    // Handle form submission
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const formData = new FormData(form);
        const data = Object.fromEntries(formData);
        
        // Validate dates
        const checkin = new Date(data.checkin);
        const checkout = new Date(data.checkout);
        
        if (checkout <= checkin) {
            alert('Check-out date must be after check-in date!');
            return;
        }
        
        // Submit booking
        try {
            const response = await fetch('http://localhost:8000/api/book', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams(data).toString()
            });
            
            const result = await response.json();
            
            if (result.success) {
                showConfirmation(result);
            } else {
                alert('Booking failed. Please try again.');
            }
        } catch (error) {
            console.error('Error submitting booking:', error);
            alert('Error submitting booking. Please make sure the server is running.');
        }
    });
}

// Show confirmation page
function showConfirmation(booking) {
    document.getElementById('homePage').style.display = 'none';
    document.getElementById('bookingPage').style.display = 'none';
    document.getElementById('confirmationPage').style.display = 'block';
    
    // Format dates
    const checkinDate = new Date(booking.checkIn).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    });
    
    const checkoutDate = new Date(booking.checkOut).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    });
    
    // Populate confirmation details
    document.getElementById('confirmBookingId').textContent = booking.bookingId;
    document.getElementById('confirmName').textContent = booking.name;
    document.getElementById('confirmRoomType').textContent = booking.roomType;
    document.getElementById('confirmCheckIn').textContent = checkinDate;
    document.getElementById('confirmCheckOut').textContent = checkoutDate;
    document.getElementById('confirmGuests').textContent = booking.guests;
    document.getElementById('confirmNights').textContent = booking.nights;
    document.getElementById('confirmTotal').textContent = `₹${booking.totalPrice.toLocaleString('en-IN')}`;
    
    // Scroll to top
    window.scrollTo(0, 0);
}