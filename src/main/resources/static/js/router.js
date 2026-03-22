const Router = {
    routes: {},
    currentPage: null,

    register(path, renderFn) {
        this.routes[path] = renderFn;
    },

    navigate(path, params = {}) {
        const query = new URLSearchParams(params).toString();
        window.location.hash = query ? `${path}?${query}` : path;
        this.render(path, params);
    },

    render(path, params = {}) {
        const [pathBase, queryString] = path.split('?');
        let mergedParams = { ...params };
        if (queryString) {
            const parsed = Object.fromEntries(new URLSearchParams(queryString));
            mergedParams = { ...parsed, ...params };
        }
        if (mergedParams.classroomId) {
            mergedParams.classroomId = parseInt(mergedParams.classroomId);
        }
        const renderFn = this.routes[pathBase];
        if (renderFn) {
            const content = document.getElementById('content');
            content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
            this.currentPage = pathBase;
            renderFn(content, mergedParams);
        }
    },

    init() {
        window.addEventListener('hashchange', () => {
            const hash = window.location.hash.slice(1) || 'home';
            this.render(hash);
        });

        const hash = window.location.hash.slice(1) || 'home';
        this.render(hash);
    }
};

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

function showModal(html) {
    const overlay = document.getElementById('modal-overlay');
    overlay.innerHTML = `<div class="modal">${html}</div>`;
    overlay.classList.remove('hidden');
}

function hideModal() {
    const overlay = document.getElementById('modal-overlay');
    overlay.classList.add('hidden');
    overlay.innerHTML = '';
}

function formatDate(dateStr) {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
