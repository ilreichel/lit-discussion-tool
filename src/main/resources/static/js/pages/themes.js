let themesState = {
    currentTheme: null,
    allBooks: [],
    selectedBookId: null,
    classroomId: null,
};

function renderThemesPage(container, params) {
    themesState.currentTheme = params.themeName || null;
    themesState.classroomId = params.classroomId || null;

    container.innerHTML = `
        <div class="themes-page">
            <div class="themes-topbar">
                <button class="btn btn-back" id="btn-themes-back" aria-label="Go back">&larr; Back</button>
                <h2 id="themes-title">${themesState.currentTheme ? escapeHtml(themesState.currentTheme) : 'Themes'}</h2>
                <div></div>
            </div>
            <div class="themes-main" id="themes-main">
                <div class="loading"><div class="spinner"></div></div>
            </div>
            <div class="themes-filter-panel" id="themes-filter-panel" style="position:fixed;">
                <h4>Filter by Book</h4>
                <label>
                    <input type="checkbox" id="themes-select-all" checked> Select All
                </label>
                <div id="themes-book-filters"></div>
            </div>
        </div>
    `;

    document.getElementById('btn-themes-back').addEventListener('click', () => {
        Router.navigate('discussion', { classroomId: themesState.classroomId });
    });

    document.getElementById('themes-select-all').addEventListener('change', (e) => {
        const checkboxes = document.querySelectorAll('#themes-book-filters input[type="checkbox"]');
        checkboxes.forEach(cb => { cb.checked = e.target.checked; });
        loadThemePosts();
    });

    loadThemesData();
}

async function loadThemesData() {
    try {
        const books = await API.getAllBooks();
        themesState.allBooks = books;
        renderThemesBookFilters(books);

        if (themesState.currentTheme) {
            loadThemePosts();
        } else {
            loadAllThemes();
        }
    } catch (err) {
        document.getElementById('themes-main').innerHTML = `
            <div class="empty-state"><h3>Error</h3><p>${escapeHtml(err.message)}</p></div>
        `;
    }
}

function renderThemesBookFilters(books) {
    const container = document.getElementById('themes-book-filters');
    container.innerHTML = books.map(b => `
        <label>
            <input type="checkbox" class="themes-book-cb" value="${b.id}" checked>
            ${escapeHtml(b.title)}
        </label>
    `).join('');

    container.querySelectorAll('.themes-book-cb').forEach(cb => {
        cb.addEventListener('change', () => {
            updateThemesSelectAll();
            loadThemePosts();
        });
    });
}

function updateThemesSelectAll() {
    const all = document.querySelectorAll('.themes-book-cb');
    const checked = document.querySelectorAll('.themes-book-cb:checked');
    document.getElementById('themes-select-all').checked = all.length === checked.length;
}

async function loadAllThemes() {
    const main = document.getElementById('themes-main');
    try {
        const themes = await API.getThemes();
        if (themes.length === 0) {
            main.innerHTML = `<div class="empty-state"><h3>No themes yet</h3><p>Create posts with tags to see themes.</p></div>`;
            return;
        }
        main.innerHTML = `
            <div class="class-grid">
                ${themes.map(t => `
                    <div class="class-card" data-theme="${escapeHtml(t.name)}" tabindex="0" role="button">
                        <h3>${escapeHtml(t.name)}</h3>
                        <p>Click to view discussions</p>
                    </div>
                `).join('')}
            </div>
        `;
        main.querySelectorAll('.class-card').forEach(card => {
            card.addEventListener('click', () => {
                themesState.currentTheme = card.dataset.theme;
                document.getElementById('themes-title').textContent = card.dataset.theme;
                loadThemePosts();
            });
        });
    } catch (err) {
        main.innerHTML = `<div class="empty-state"><h3>Error</h3><p>${escapeHtml(err.message)}</p></div>`;
    }
}

async function loadThemePosts() {
    if (!themesState.currentTheme) return;

    const main = document.getElementById('themes-main');
    main.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    const checkedBooks = document.querySelectorAll('.themes-book-cb:checked');
    const bookIds = Array.from(checkedBooks).map(cb => parseInt(cb.value));

    try {
        let allPosts = [];
        if (bookIds.length === themesState.allBooks.length || bookIds.length === 0) {
            allPosts = await API.getPostsForTheme(themesState.currentTheme);
        } else {
            const results = await Promise.all(
                bookIds.map(id => API.getPostsForTheme(themesState.currentTheme, id))
            );
            const seen = new Set();
            results.flat().forEach(p => {
                if (!seen.has(p.id)) {
                    seen.add(p.id);
                    allPosts.push(p);
                }
            });
            allPosts.sort((a, b) => {
                if (a.chapterNumber !== b.chapterNumber) return a.chapterNumber - b.chapterNumber;
                return new Date(a.publishedAt) - new Date(b.publishedAt);
            });
        }

        if (allPosts.length === 0) {
            main.innerHTML = `
                <div class="empty-state">
                    <h3>No posts found for "${escapeHtml(themesState.currentTheme)}"</h3>
                    <p>No discussion posts match this theme with the selected filters.</p>
                </div>
            `;
            return;
        }

        const grouped = {};
        allPosts.forEach(post => {
            const key = `${post.bookTitle} - Ch ${post.chapterNumber}`;
            if (!grouped[key]) {
                grouped[key] = {
                    bookTitle: post.bookTitle,
                    chapterNumber: post.chapterNumber,
                    chapterTitle: post.chapterTitle,
                    posts: [],
                };
            }
            grouped[key].posts.push(post);
        });

        const sortedGroups = Object.values(grouped).sort((a, b) => a.chapterNumber - b.chapterNumber);

        main.innerHTML = `
            <h3 style="margin-bottom:16px;color:var(--color-teal);">${escapeHtml(themesState.currentTheme)}</h3>
            ${sortedGroups.map(g => `
                <div class="chapter-section">
                    <div class="chapter-header">${escapeHtml(g.bookTitle)} - ${escapeHtml(g.chapterTitle)}</div>
                    ${g.posts.map(post => renderPostCard(post)).join('')}
                </div>
            `).join('')}
        `;
    } catch (err) {
        main.innerHTML = `<div class="empty-state"><h3>Error</h3><p>${escapeHtml(err.message)}</p></div>`;
    }
}
