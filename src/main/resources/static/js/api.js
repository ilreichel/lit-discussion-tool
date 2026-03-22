const API = {
    baseUrl: '/api',

    async request(method, path, body = null) {
        const options = {
            method,
            headers: { 'Content-Type': 'application/json' },
        };
        if (body) {
            options.body = JSON.stringify(body);
        }
        const response = await fetch(this.baseUrl + path, options);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'Request failed');
        }
        return data;
    },

    login(username) {
        return this.request('POST', '/login', { username });
    },

    getClasses(username) {
        return this.request('GET', `/classes?username=${encodeURIComponent(username)}`);
    },

    getBooksForClass(classroomId) {
        return this.request('GET', `/classes/${classroomId}/books`);
    },

    getPostsForBook(bookId) {
        return this.request('GET', `/books/${bookId}/posts`);
    },

    createPost(data) {
        return this.request('POST', '/posts', data);
    },

    detectQuotes(text) {
        return this.request('POST', '/detect-quotes', { text });
    },

    getThemes() {
        return this.request('GET', '/themes');
    },

    getPostsForTheme(themeName, bookId = null) {
        const params = bookId ? `?bookId=${bookId}` : '';
        return this.request('GET', `/themes/${encodeURIComponent(themeName)}/posts${params}`);
    },

    getVisualization(bookId = null) {
        const params = bookId ? `?bookId=${bookId}` : '';
        return this.request('GET', `/visualization${params}`);
    },

    getAllBooks() {
        return this.request('GET', '/books');
    },
};
