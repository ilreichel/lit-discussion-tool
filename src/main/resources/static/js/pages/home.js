function renderHomePage(container, params) {
    if (!AppState.username) {
        container.innerHTML = `
            <div class="home-page">
                <h2 class="section-title">My Classes</h2>
                <div class="class-grid">
                    <div class="empty-state">
                        <h3>Please log in</h3>
                        <p>Enter your username and click Login to see your classes.</p>
                    </div>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="home-page">
            <h2 class="section-title">My Classes</h2>
            <div class="class-grid" id="class-grid">
                <div class="loading"><div class="spinner"></div></div>
            </div>
        </div>
    `;

    loadClasses(AppState.username);
}

async function loadClasses(username) {
    try {
        const classes = await API.getClasses(username);
        const grid = document.getElementById('class-grid');

        if (classes.length === 0) {
            grid.innerHTML = `
                <div class="empty-state">
                    <h3>No classes found</h3>
                    <p>You are not enrolled in any classes yet.</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = classes.map(c => `
            <div class="class-card" data-class-id="${c.id}" tabindex="0" role="button"
                 aria-label="Open class ${escapeHtml(c.name)}">
                <h3>${escapeHtml(c.name)}</h3>
                <p>${escapeHtml(c.description || '')}</p>
            </div>
        `).join('');

        grid.querySelectorAll('.class-card').forEach(card => {
            card.addEventListener('click', () => {
                if (!AppState.username) {
                    showToast('Please log in first', 'error');
                    return;
                }
                const classId = card.dataset.classId;
                Router.navigate('discussion', { classroomId: classId });
            });
            card.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    card.click();
                }
            });
        });
    } catch (err) {
        document.getElementById('class-grid').innerHTML = `
            <div class="empty-state">
                <h3>Error loading classes</h3>
                <p>${escapeHtml(err.message)}</p>
            </div>
        `;
    }
}
