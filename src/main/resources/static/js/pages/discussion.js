let discussionState = {
    classroomId: null,
    books: [],
    currentBookId: null,
    posts: [],
};

function renderDiscussionPage(container, params) {
    discussionState.classroomId = params.classroomId || null;

    if (!discussionState.classroomId) {
        container.innerHTML = '<div class="empty-state"><h3>No class selected</h3></div>';
        return;
    }

    container.innerHTML = `
        <div class="discussion-page">
            <aside class="discussion-sidebar" id="discussion-sidebar">
                <div class="loading"><div class="spinner"></div></div>
            </aside>
            <div class="discussion-main">
                <div class="discussion-header">
                    <h2 id="discussion-title">Loading...</h2>
                    <button class="btn btn-data" id="btn-data" aria-label="View data visualizations">Data</button>
                </div>
                <div class="discussion-posts" id="discussion-posts">
                    <div class="loading"><div class="spinner"></div></div>
                </div>
            </div>
        </div>
        <button class="btn btn-create-entry" id="btn-create-entry" aria-label="Create new discussion entry">Create Entry</button>
    `;

    document.getElementById('btn-data').addEventListener('click', () => {
        Router.navigate('visualizations', { classroomId: discussionState.classroomId });
    });

    document.getElementById('btn-create-entry').addEventListener('click', () => {
        showCreateEntryModal();
    });

    loadDiscussionBooks();
}

async function loadDiscussionBooks() {
    try {
        const books = await API.getBooksForClass(discussionState.classroomId);
        discussionState.books = books;
        renderSidebar(books);

        if (books.length > 0) {
            selectBook(books[0].id);
        }
    } catch (err) {
        document.getElementById('discussion-sidebar').innerHTML = `
            <div class="empty-state"><h3>Error loading books</h3><p>${escapeHtml(err.message)}</p></div>
        `;
    }
}

function renderSidebar(books) {
    const sidebar = document.getElementById('discussion-sidebar');

    if (books.length === 0) {
        sidebar.innerHTML = `
            <div class="empty-state">
                <h3>No books assigned</h3>
                <p>No books are available for this class.</p>
            </div>
        `;
        return;
    }

    sidebar.innerHTML = books.map(book => `
        <div class="sidebar-book">
            <div class="sidebar-book-title" data-book-id="${book.id}" tabindex="0" role="button">
                ${escapeHtml(book.title)}
            </div>
            <div class="sidebar-chapters" id="chapters-${book.id}">
                ${book.chapters && book.chapters.length > 0
                    ? book.chapters.map(ch => `
                        <div class="sidebar-chapter" data-chapter-id="${ch.id}" data-chapter-num="${ch.chapterNumber}"
                             tabindex="0" role="button">
                            ${escapeHtml(ch.title)}
                        </div>
                    `).join('')
                    : '<div class="sidebar-chapter" style="color:#999;cursor:default;">No chapters loaded</div>'
                }
            </div>
        </div>
    `).join('');

    sidebar.querySelectorAll('.sidebar-book-title').forEach(el => {
        el.addEventListener('click', () => selectBook(el.dataset.bookId));
        el.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') el.click();
        });
    });

    sidebar.querySelectorAll('.sidebar-chapter[data-chapter-id]').forEach(el => {
        el.addEventListener('click', () => {
            const chapterId = el.dataset.chapterId;
            const chapterEl = document.getElementById(`chapter-section-${chapterId}`);
            if (chapterEl) {
                chapterEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
            sidebar.querySelectorAll('.sidebar-chapter').forEach(c => c.classList.remove('active'));
            el.classList.add('active');
        });
        el.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') el.click();
        });
    });
}

async function selectBook(bookId) {
    discussionState.currentBookId = bookId;
    const book = discussionState.books.find(b => b.id == bookId);
    document.getElementById('discussion-title').textContent = book ? book.title : 'Discussion';

    const postsContainer = document.getElementById('discussion-posts');
    postsContainer.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

    try {
        const posts = await API.getPostsForBook(bookId);
        discussionState.posts = posts;
        renderPosts(posts, book);
    } catch (err) {
        postsContainer.innerHTML = `
            <div class="empty-state"><h3>Error loading posts</h3><p>${escapeHtml(err.message)}</p></div>
        `;
    }
}

function renderPosts(posts, book) {
    const container = document.getElementById('discussion-posts');

    if (posts.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>No discussion posts yet</h3>
                <p>Be the first to start a discussion about this book!</p>
            </div>
        `;
        return;
    }

    const grouped = {};
    posts.forEach(post => {
        const chKey = post.chapterId;
        if (!grouped[chKey]) {
            grouped[chKey] = {
                chapterNumber: post.chapterNumber,
                chapterTitle: post.chapterTitle,
                posts: [],
            };
        }
        grouped[chKey].posts.push(post);
    });

    const sortedChapters = Object.values(grouped).sort((a, b) => a.chapterNumber - b.chapterNumber);

    container.innerHTML = sortedChapters.map(ch => `
        <div class="chapter-section" id="chapter-section-${ch.posts[0].chapterId}">
            <div class="chapter-header">${escapeHtml(ch.chapterTitle)}</div>
            ${ch.posts.map(post => renderPostCard(post)).join('')}
        </div>
    `).join('');
}

function renderPostCard(post) {
    const quotesHtml = post.quotes.length > 0
        ? `<div class="post-quotes-section">${post.quotes.map((q, i) => {
            const raw = q.text;
            const clean = raw.replace(/^["\u201C]+/, '').replace(/["\u201D]+$/, '');
            return `
            <div class="post-quote-block">
                <div class="post-quote">"${escapeHtml(clean)}"</div>
                <div class="post-themes">
                    ${q.themes.map((t, j) => `<span class="theme-chip" data-color="${j % 4}" onclick="navigateToTheme('${escapeHtml(t)}')">${escapeHtml(t)}</span>`).join('')}
                </div>
            </div>`;
        }).join('')}</div>`
        : '';

    return `
        <article class="post-card">
            <div class="post-meta">
                <span class="post-author">${escapeHtml(post.author)}</span>
                <time class="post-time">${formatDate(post.publishedAt)}</time>
            </div>
            <div class="post-content">${escapeHtml(post.content)}</div>
            ${quotesHtml}
        </article>
    `;
}

function navigateToTheme(themeName) {
    Router.navigate('themes', { themeName, classroomId: discussionState.classroomId });
}

function showCreateEntryModal() {
    const book = discussionState.books.find(b => b.id == discussionState.currentBookId);
    if (!book || !book.chapters || book.chapters.length === 0) {
        showToast('No chapters available for this book', 'error');
        return;
    }

    const chaptersOptions = book.chapters.map(ch =>
        `<option value="${ch.id}">${escapeHtml(ch.title)}</option>`
    ).join('');

    showModal(`
        <div class="modal-header">
            <h2>Create Discussion Entry</h2>
            <button class="modal-close" onclick="hideModal()" aria-label="Close">&times;</button>
        </div>
        <div class="modal-body">
            <div class="form-group">
                <label for="entry-chapter">Chapter</label>
                <select id="entry-chapter">${chaptersOptions}</select>
            </div>
            <div class="form-group">
                <label for="entry-content">Discussion Text</label>
                <textarea id="entry-content" placeholder="Write your discussion here. Use \"quotes\" to automatically detect quotations."></textarea>
                <div class="form-error" id="content-error"></div>
            </div>
            <div class="form-group">
                <label>Detected Quotations</label>
                <button class="btn btn-secondary" id="btn-detect-quotes" type="button" style="margin-bottom:12px;">Detect Quotes</button>
                <div id="quotes-container"></div>
                <div class="form-error" id="quotes-error"></div>
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-outline" onclick="hideModal()">Cancel</button>
            <button class="btn btn-primary" id="btn-submit-entry">Submit</button>
        </div>
    `);

    document.getElementById('btn-detect-quotes').addEventListener('click', handleDetectQuotes);
    document.getElementById('btn-submit-entry').addEventListener('click', handleSubmitEntry);
}

let detectedQuotes = [];

async function handleDetectQuotes() {
    const content = document.getElementById('entry-content').value;
    if (!content.trim()) {
        showToast('Please enter discussion text first', 'error');
        return;
    }

    try {
        const result = await API.detectQuotes(content);
        detectedQuotes = result.quotes.map(q => ({ text: q, themeNames: [] }));

        const container = document.getElementById('quotes-container');

        if (detectedQuotes.length === 0) {
            container.innerHTML = '<p style="color:#999;font-size:0.85rem;">No quotes detected. Use "quotation marks" around quoted text.</p>';
            return;
        }

        container.innerHTML = detectedQuotes.map((q, i) => `
            <div class="quote-entry" data-index="${i}">
                <div class="quote-entry-header">
                    <h4>Quote ${i + 1}</h4>
                </div>
                <div class="quote-text-display">"${escapeHtml(q.text)}"</div>
                <div class="form-group">
                    <label>Tags/Themes (required)</label>
                    <div class="tag-input-row">
                        <input type="text" class="tag-input" placeholder="Enter a tag and press Add"
                               data-index="${i}" aria-label="Add tag for quote ${i + 1}">
                        <button class="btn-add-tag" data-index="${i}" type="button">Add</button>
                    </div>
                    <div class="tags-display" id="tags-display-${i}"></div>
                </div>
            </div>
        `).join('');

        container.querySelectorAll('.btn-add-tag').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.dataset.index);
                const input = container.querySelector(`.tag-input[data-index="${idx}"]`);
                addTag(idx, input.value.trim());
                input.value = '';
            });
        });

        container.querySelectorAll('.tag-input').forEach(input => {
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const idx = parseInt(input.dataset.index);
                    addTag(idx, input.value.trim());
                    input.value = '';
                }
            });
        });
    } catch (err) {
        showToast('Error detecting quotes: ' + err.message, 'error');
    }
}

function addTag(quoteIndex, tagName) {
    if (!tagName) return;
    const quote = detectedQuotes[quoteIndex];
    if (quote.themeNames.includes(tagName)) {
        showToast('Tag already added', 'info');
        return;
    }
    quote.themeNames.push(tagName);
    renderTags(quoteIndex);
}

function removeTag(quoteIndex, tagName) {
    detectedQuotes[quoteIndex].themeNames = detectedQuotes[quoteIndex].themeNames.filter(t => t !== tagName);
    renderTags(quoteIndex);
}

function renderTags(quoteIndex) {
    const display = document.getElementById(`tags-display-${quoteIndex}`);
    const tags = detectedQuotes[quoteIndex].themeNames;
    display.innerHTML = tags.map(t => `
        <span class="tag-chip">
            ${escapeHtml(t)}
            <span class="remove-tag" onclick="removeTag(${quoteIndex}, '${escapeHtml(t)}')" role="button" aria-label="Remove tag ${escapeHtml(t)}">&times;</span>
        </span>
    `).join('');
}

async function handleSubmitEntry() {
    const content = document.getElementById('entry-content').value.trim();
    const chapterId = parseInt(document.getElementById('entry-chapter').value);
    const contentError = document.getElementById('content-error');
    const quotesError = document.getElementById('quotes-error');

    contentError.textContent = '';
    quotesError.textContent = '';

    if (!content) {
        contentError.textContent = 'Discussion content is required.';
        return;
    }

    if (detectedQuotes.length === 0) {
        quotesError.textContent = 'At least one quotation is required. Click "Detect Quotes" first.';
        return;
    }

    let hasError = false;
    for (let i = 0; i < detectedQuotes.length; i++) {
        const q = detectedQuotes[i];
        if (q.text.split(/\s+/).length <= 2) {
            quotesError.textContent = `Quote ${i + 1} must contain more than two words.`;
            hasError = true;
            break;
        }
        if (q.themeNames.length === 0) {
            quotesError.textContent = `Quote ${i + 1} needs at least one tag/theme.`;
            hasError = true;
            break;
        }
    }

    if (hasError) return;

    const username = localStorage.getItem('username') || 'student1';

    try {
        await API.createPost({
            username,
            classroomId: discussionState.classroomId,
            bookId: discussionState.currentBookId,
            chapterId,
            content,
            quotes: detectedQuotes,
        });

        hideModal();
        showToast('Discussion post created!', 'success');
        detectedQuotes = [];
        selectBook(discussionState.currentBookId);
    } catch (err) {
        quotesError.textContent = err.message;
    }
}
