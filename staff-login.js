// Check if already logged in
document.addEventListener('DOMContentLoaded', () => {
    const isLoggedIn = sessionStorage.getItem('staffLoggedIn');
    if (isLoggedIn === 'true') {
        window.location.href = 'staff-dashboard.html';
    }
});

// Handle login form submission
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.classList.remove('show');
    
    const formData = new FormData(e.target);
    const username = formData.get('username');
    const password = formData.get('password');
    
    try {
        const response = await fetch('http://localhost:8000/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({ username, password }).toString()
        });
        
        const result = await response.json();
        
        if (result.success) {
            // Store login state
            sessionStorage.setItem('staffLoggedIn', 'true');
            sessionStorage.setItem('staffUsername', username);
            
            // Show success message briefly
            errorMessage.textContent = '✓ Login successful! Redirecting...';
            errorMessage.style.backgroundColor = '#d4edda';
            errorMessage.style.color = '#155724';
            errorMessage.classList.add('show');
            
            // Redirect to dashboard
            setTimeout(() => {
                window.location.href = 'staff-dashboard.html';
            }, 1000);
        } else {
            // Show error message
            errorMessage.textContent = '✗ ' + result.message;
            errorMessage.classList.add('show');
            
            // Clear password field
            document.getElementById('password').value = '';
            document.getElementById('password').focus();
        }
    } catch (error) {
        console.error('Login error:', error);
        errorMessage.textContent = '✗ Server error. Please make sure the server is running.';
        errorMessage.classList.add('show');
    }
});

// Allow Enter key to submit
document.getElementById('password').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        document.getElementById('loginForm').dispatchEvent(new Event('submit'));
    }
});