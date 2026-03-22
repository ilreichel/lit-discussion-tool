document.addEventListener('DOMContentLoaded', () => {
    const usernameInput = document.getElementById('username-input');
    const loginBtn = document.getElementById('login-btn');

    const storedUsername = localStorage.getItem('username');
    if (storedUsername) {
        usernameInput.value = storedUsername;
    }

    loginBtn.addEventListener('click', handleLogin);
    usernameInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') handleLogin();
    });

    async function handleLogin() {
        const username = usernameInput.value.trim();
        if (!username) {
            showToast('Please enter a username', 'error');
            return;
        }

        try {
            const user = await API.login(username);
            localStorage.setItem('username', user.username);
            showToast(`Welcome, ${user.displayName}!`, 'success');
            Router.navigate('home');
        } catch (err) {
            showToast(err.message || 'Login failed', 'error');
        }
    }

    Router.register('home', renderHomePage);
    Router.register('discussion', renderDiscussionPage);
    Router.register('visualizations', renderVisualizationsPage);
    Router.register('themes', renderThemesPage);

    document.getElementById('modal-overlay').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) hideModal();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') hideModal();
    });

    Router.init();
});
