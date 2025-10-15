// Check authentication
document.addEventListener('DOMContentLoaded', () => {
    const isLoggedIn = sessionStorage.getItem('staffLoggedIn');
    if (isLoggedIn !== 'true') {
        window.location.href = 'staff-login.html';
        return;
    }
    
    loadBookings();
    setupFormHandlers();
});

// Load all bookings from backend
async function loadBookings() {
    try {
        const response = await fetch('http://localhost:8000/api/bookings');
        const bookings = await response.json();
        
        displayBookings(bookings);
        updateStats(bookings);
    } catch (error) {
        console.error('Error loading bookings:', error);
        document.getElementById('bookingsTableBody').innerHTML = 
            '<tr><td colspan="8" class="no-data" style="color: red;">Failed to load bookings. Please ensure the server is running.</td></tr>';
    }
}

// Display bookings in table
function displayBookings(bookings) {
    const tbody = document.getElementById('bookingsTableBody');
    
    if (bookings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="no-data">No bookings found. Add your first booking!</td></tr>';
        return;
    }
    
    tbody.innerHTML = bookings.map(booking => {
        const checkInDate = new Date(booking.checkIn).toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
        
        const checkOutDate = new Date(booking.checkOut).toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
        
        return `
            <tr>
                <td><strong>#${booking.bookingId}</strong></td>
                <td>${booking.name}</td>
                <td>${booking.roomType}</td>
                <td><strong style="color: #667eea;">Room ${booking.roomNumber}</strong></td>
                <td>${checkInDate}</td>
                <td>${checkOutDate}</td>
                <td>${booking.guests}</td>
                <td><strong>₹${booking.totalPrice.toLocaleString('en-IN')}</strong></td>
                <td>
                    <button class="action-btn view-btn" onclick="viewDetails(${booking.bookingId})">View</button>
                    <button class="action-btn edit-btn" onclick="editBooking(${booking.bookingId})">Edit</button>
                    <button class="action-btn delete-btn" onclick="deleteBooking(${booking.bookingId})">Delete</button>
                </td>
            </tr>
        `;
    }).join('');
}

// Update statistics
function updateStats(bookings) {
    const totalBookings = bookings.length;
    const totalRevenue = bookings.reduce((sum, b) => sum + b.totalPrice, 0);
    
    const today = new Date().toISOString().split('T')[0];
    const todayCheckIns = bookings.filter(b => b.checkIn === today).length;
    
    document.getElementById('totalBookings').textContent = totalBookings;
    document.getElementById('totalRevenue').textContent = '₹' + totalRevenue.toLocaleString('en-IN');
    document.getElementById('todayCheckIns').textContent = todayCheckIns;
}

// View booking details
async function viewDetails(bookingId) {
    try {
        const response = await fetch('http://localhost:8000/api/bookings');
        const bookings = await response.json();
        const booking = bookings.find(b => b.bookingId === bookingId);
        
        if (booking) {
            const checkInDate = new Date(booking.checkIn).toLocaleDateString('en-IN', {
                day: 'numeric',
                month: 'long',
                year: 'numeric'
            });
            
            const checkOutDate = new Date(booking.checkOut).toLocaleDateString('en-IN', {
                day: 'numeric',
                month: 'long',
                year: 'numeric'
            });
            
            document.getElementById('bookingDetails').innerHTML = `
                <div class="detail-item">
                    <span class="detail-label">Booking ID:</span>
                    <span class="detail-value">#${booking.bookingId}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Guest Name:</span>
                    <span class="detail-value">${booking.name}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Room Type:</span>
                    <span class="detail-value">${booking.roomType}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Check-in Date:</span>
                    <span class="detail-value">${checkInDate}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Check-out Date:</span>
                    <span class="detail-value">${checkOutDate}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Number of Guests:</span>
                    <span class="detail-value">${booking.guests}</span>
                </div>
                <div class="detail-item">
                    <span class="detail-label">Number of Nights:</span>
                    <span class="detail-value">${booking.nights}</span>
                </div>
                <div class="detail-item total-item">
                    <span class="detail-label">Total Amount:</span>
                    <span class="detail-value">₹${booking.totalPrice.toLocaleString('en-IN')}</span>
                </div>
            `;
            
            document.getElementById('detailsModal').style.display = 'flex';
        }
    } catch (error) {
        console.error('Error fetching booking details:', error);
        alert('Failed to load booking details.');
    }
}

// Edit booking
async function editBooking(bookingId) {
    try {
        const response = await fetch('http://localhost:8000/api/bookings');
        const bookings = await response.json();
        const booking = bookings.find(b => b.bookingId === bookingId);
        
        if (booking) {
            document.getElementById('editBookingId').value = booking.bookingId;
            document.getElementById('editGuestName').value = booking.name;
            document.getElementById('editCheckIn').value = booking.checkIn;
            document.getElementById('editCheckOut').value = booking.checkOut;
            document.getElementById('editGuests').value = booking.guests;
            document.getElementById('editRoomType').value = booking.roomType;
            
            document.getElementById('editModal').style.display = 'flex';
        }
    } catch (error) {
        console.error('Error fetching booking:', error);
        alert('Failed to load booking for editing.');
    }
}

// Delete booking
async function deleteBooking(bookingId) {
    if (!confirm('Are you sure you want to delete this booking?')) {
        return;
    }
    
    try {
        const response = await fetch('http://localhost:8000/api/bookings/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({ bookingId: bookingId }).toString()
        });
        
        const result = await response.json();
        
        if (result.success) {
            alert('Booking deleted successfully!');
            loadBookings();
        } else {
            alert('Failed to delete booking: ' + result.message);
        }
    } catch (error) {
        console.error('Error deleting booking:', error);
        alert('Error deleting booking. Please try again.');
    }
}

// Setup form handlers
function setupFormHandlers() {
    // Add Booking Form
    document.getElementById('addBookingForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData);
        
        const checkin = new Date(data.checkin);
        const checkout = new Date(data.checkout);
        
        if (checkout <= checkin) {
            alert('Check-out date must be after check-in date!');
            return;
        }
        
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
                alert('Booking added successfully!');
                e.target.reset();
                showDashboard();
                loadBookings();
            } else {
                alert('Failed to add booking. Please try again.');
            }
        } catch (error) {
            console.error('Error adding booking:', error);
            alert('Error adding booking. Please try again.');
        }
    });
    
    // Edit Booking Form
    document.getElementById('editBookingForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData);
        
        const checkin = new Date(data.checkin);
        const checkout = new Date(data.checkout);
        
        if (checkout <= checkin) {
            alert('Check-out date must be after check-in date!');
            return;
        }
        
        try {
            const response = await fetch('http://localhost:8000/api/bookings/update', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams(data).toString()
            });
            
            const result = await response.json();
            
            if (result.success) {
                alert('Booking updated successfully!');
                closeEditModal();
                loadBookings();
            } else {
                alert('Failed to update booking: ' + result.message);
            }
        } catch (error) {
            console.error('Error updating booking:', error);
            alert('Error updating booking. Please try again.');
        }
    });
    
    // Date validation
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('checkInDate').min = today;
    document.getElementById('checkOutDate').min = today;
}

// Navigation functions
function showDashboard() {
    document.getElementById('dashboardPage').style.display = 'block';
    document.getElementById('addBookingPage').style.display = 'none';
    loadBookings();
}

function showAddBooking() {
    document.getElementById('dashboardPage').style.display = 'none';
    document.getElementById('addBookingPage').style.display = 'block';
}

// Logout function
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        // Clear session storage
        sessionStorage.removeItem('staffLoggedIn');
        sessionStorage.removeItem('staffUsername');
        
        // Redirect to login page
        window.location.href = 'staff-login.html';
    }
}

// Modal functions
function closeDetailsModal() {
    document.getElementById('detailsModal').style.display = 'none';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

// Close modals when clicking outside
window.onclick = function(event) {
    const detailsModal = document.getElementById('detailsModal');
    const editModal = document.getElementById('editModal');
    
    if (event.target === detailsModal) {
        closeDetailsModal();
    }
    if (event.target === editModal) {
        closeEditModal();
    }
}