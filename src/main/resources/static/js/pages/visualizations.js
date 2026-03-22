let vizState = {
    simulation: null,
    svg: null,
    nodes: [],
    edges: [],
    selectedBooks: new Set(),
    allBooks: [],
    classroomId: null,
};

function renderVisualizationsPage(container, params) {
    vizState.classroomId = params.classroomId || null;
    container.innerHTML = `
        <div class="viz-page">
            <div class="viz-topbar">
                <button class="btn btn-back" id="btn-viz-back" aria-label="Go back">&larr; Back</button>
                <h2>Theme Relationships</h2>
                <div></div>
            </div>
            <div class="viz-container" id="viz-container">
                <div class="loading"><div class="spinner"></div></div>
            </div>
            <div class="viz-filter-panel" id="viz-filter-panel">
                <h4>Filter by Book</h4>
                <label>
                    <input type="checkbox" id="viz-select-all" checked> Select All
                </label>
                <div id="viz-book-filters"></div>
            </div>
        </div>
    `;

    document.getElementById('btn-viz-back').addEventListener('click', () => {
        Router.navigate('discussion', { classroomId: vizState.classroomId });
    });

    document.getElementById('viz-select-all').addEventListener('change', (e) => {
        const checkboxes = document.querySelectorAll('#viz-book-filters input[type="checkbox"]');
        checkboxes.forEach(cb => {
            cb.checked = e.target.checked;
        });
        updateVizFilter();
    });

    loadVizData();
}

async function loadVizData() {
    try {
        const [books, vizData] = await Promise.all([
            API.getAllBooks(),
            API.getVisualization(),
        ]);

        vizState.allBooks = books;
        vizState.nodes = vizData.nodes;
        vizState.edges = vizData.edges;

        renderBookFilters(books);
        renderVisualization(vizData);
    } catch (err) {
        document.getElementById('viz-container').innerHTML = `
            <div class="empty-state"><h3>Error loading visualization</h3><p>${escapeHtml(err.message)}</p></div>
        `;
    }
}

function renderBookFilters(books) {
    const container = document.getElementById('viz-book-filters');
    container.innerHTML = books.map(b => `
        <label>
            <input type="checkbox" class="viz-book-cb" value="${b.id}" checked>
            ${escapeHtml(b.title)}
        </label>
    `).join('');

    container.querySelectorAll('.viz-book-cb').forEach(cb => {
        cb.addEventListener('change', updateVizFilter);
    });
}

async function updateVizFilter() {
    const selectAll = document.getElementById('viz-select-all');
    const checkboxes = document.querySelectorAll('.viz-book-cb:checked');
    const selectedIds = Array.from(checkboxes).map(cb => parseInt(cb.value));

    if (selectedIds.length === 0) {
        renderVisualization({ nodes: [], edges: [] });
        return;
    }

    if (selectedIds.length === vizState.allBooks.length) {
        selectAll.checked = true;
        renderVisualization({ nodes: vizState.nodes, edges: vizState.edges });
        return;
    }

    selectAll.checked = false;

    try {
        const results = await Promise.all(selectedIds.map(id => API.getVisualization(id)));
        const mergedNodes = new Map();
        const mergedEdges = new Map();

        results.forEach(data => {
            data.nodes.forEach(n => {
                if (mergedNodes.has(n.id)) {
                    const existing = mergedNodes.get(n.id);
                    existing.frequency += n.frequency;
                    existing.size = Math.max(existing.size, n.size);
                } else {
                    mergedNodes.set(n.id, { ...n });
                }
            });
            data.edges.forEach(e => {
                const key = e.source < e.target ? `${e.source}|||${e.target}` : `${e.target}|||${e.source}`;
                if (mergedEdges.has(key)) {
                    const existing = mergedEdges.get(key);
                    existing.weight += e.weight;
                    existing.similarity = Math.max(existing.similarity, e.similarity);
                } else {
                    mergedEdges.set(key, { ...e });
                }
            });
        });

        renderVisualization({
            nodes: Array.from(mergedNodes.values()),
            edges: Array.from(mergedEdges.values()),
        });
    } catch (err) {
        showToast('Error updating visualization: ' + err.message, 'error');
    }
}

function renderVisualization(data) {
    const container = document.getElementById('viz-container');

    if (data.nodes.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <h3>No theme data available</h3>
                <p>Create discussion posts with quotes and themes to see visualizations.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = '';
    const width = container.clientWidth;
    const height = container.clientHeight;

    const svg = d3.select(container)
        .append('svg')
        .attr('width', width)
        .attr('height', height);

    const g = svg.append('g');

    const zoom = d3.zoom()
        .scaleExtent([0.2, 4])
        .on('zoom', (event) => {
            g.attr('transform', event.transform);
            updateLabelsVisibility(event.transform.k);
        });

    svg.call(zoom);

    const nodes = data.nodes.map(d => ({ ...d }));
    const nodeIds = new Set(nodes.map(n => n.id));
    const edges = data.edges.map(d => ({ ...d })).filter(e => {
        return nodeIds.has(e.source) && nodeIds.has(e.target);
    });

    const maxFreq = Math.max(...nodes.map(n => n.frequency), 1);

    const simulation = d3.forceSimulation(nodes)
        .force('link', d3.forceLink(edges).id(d => d.id).distance(d => {
            return 250 * (1 - d.similarity) + 20;
        }).strength(d => {
            return d.similarity * d.similarity * 0.8;
        }))
        .force('charge', d3.forceManyBody().strength(-300).distanceMax(500))
        .force('center', d3.forceCenter(width / 2, height / 2))
        .force('collision', d3.forceCollide().radius(d => d.size / 2 + 15))
        .force('x', d3.forceX(width / 2).strength(0.05))
        .force('y', d3.forceY(height / 2).strength(0.05))
        .alphaDecay(0.02)
        .velocityDecay(0.4);

    vizState.simulation = simulation;

    const link = g.append('g')
        .selectAll('line')
        .data(edges)
        .enter()
        .append('line')
        .attr('stroke', d => {
            const sourceNode = nodes.find(n => n.id === (d.source.id || d.source));
            return sourceNode ? sourceNode.color : '#bbb';
        })
        .attr('stroke-width', d => 1 + d.similarity * 4)
        .attr('stroke-opacity', d => 0.3 + d.similarity * 0.5);

    const node = g.append('g')
        .selectAll('g')
        .data(nodes)
        .enter()
        .append('g')
        .attr('cursor', 'pointer')
        .call(d3.drag()
            .on('start', (event, d) => {
                if (!event.active) simulation.alphaTarget(0.3).restart();
                d.fx = d.x;
                d.fy = d.y;
            })
            .on('drag', (event, d) => {
                d.fx = event.x;
                d.fy = event.y;
            })
            .on('end', (event, d) => {
                if (!event.active) simulation.alphaTarget(0);
                d.fx = null;
                d.fy = null;
            })
        );

    node.append('circle')
        .attr('r', d => d.size / 2)
        .attr('fill', d => d.color)
        .attr('stroke', '#fff')
        .attr('stroke-width', 2)
        .attr('opacity', 1);

    node.append('text')
        .attr('class', 'node-label')
        .attr('text-anchor', 'middle')
        .each(function(d) {
            const el = d3.select(this);
            const radius = d.size / 2;
            const padding = 6;
            const usableR = radius - padding;
            const usableD = usableR * 2;
            const words = d.label.split(/\s+/);

            let fontSize = Math.max(8, Math.min(16, usableR * 0.38));
            const lineH = fontSize * 1.2;
            const maxLines = Math.max(1, Math.floor(usableD / lineH));
            const maxLineWidth = usableD * 0.88;

            function wrapLines(size) {
                const charW = size * 0.58;
                const maxPerLine = Math.max(2, Math.floor(maxLineWidth / charW));
                const lines = [];
                let current = '';
                for (const w of words) {
                    const test = current ? current + ' ' + w : w;
                    if (test.length > maxPerLine && current) {
                        lines.push(current);
                        current = w;
                    } else {
                        current = test;
                    }
                }
                if (current) lines.push(current);
                return lines;
            }

            let lines = wrapLines(fontSize);

            while (lines.length > maxLines && fontSize > 7) {
                fontSize -= 0.5;
                lines = wrapLines(fontSize);
            }

            el.attr('font-size', fontSize + 'px')
              .attr('fill', '#fff')
              .attr('font-weight', 600);

            if (lines.length <= maxLines) {
                const lh = fontSize * 1.2;
                const totalH = lines.length * lh;
                const startY = -(totalH - lh) / 2;
                lines.forEach((line, i) => {
                    el.append('tspan')
                        .attr('x', 0)
                        .attr('dy', i === 0 ? startY + 'px' : lh + 'px')
                        .text(line);
                });
            } else {
                const w = words[0];
                const maxC = Math.max(2, Math.floor(maxLineWidth / (fontSize * 0.58)));
                el.append('tspan')
                    .attr('x', 0)
                    .attr('dy', '0.35em')
                    .text(w.length > maxC ? w.slice(0, maxC - 1) + '\u2026' : w + '\u2026');
            }
        });

    node.on('click', (event, d) => {
        event.stopPropagation();
        Router.navigate('themes', { themeName: d.id, classroomId: vizState.classroomId });
    });

    const tooltip = document.createElement('div');
    tooltip.className = 'viz-tooltip';
    tooltip.style.display = 'none';
    container.appendChild(tooltip);

    node.on('mouseenter', (event, d) => {
        tooltip.style.display = 'block';
        tooltip.innerHTML = `<strong>${escapeHtml(d.label)}</strong><br>Frequency: ${d.frequency}`;
        d3.select(event.currentTarget).select('circle')
            .attr('stroke-width', 4)
            .attr('stroke', '#333');
    })
    .on('mousemove', (event) => {
        const rect = container.getBoundingClientRect();
        tooltip.style.left = (event.clientX - rect.left + 10) + 'px';
        tooltip.style.top = (event.clientY - rect.top - 10) + 'px';
    })
    .on('mouseleave', (event) => {
        tooltip.style.display = 'none';
        d3.select(event.currentTarget).select('circle')
            .attr('stroke-width', 2)
            .attr('stroke', '#fff');
    });

    simulation.on('tick', () => {
        link
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);

        node.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    setTimeout(() => simulation.stop(), 10000);

    function updateLabelsVisibility(k) {
        node.select('text').attr('opacity', k < 0.5 ? 0 : 1);
    }
}
