(function () {
    'use strict';

    var root = document.getElementById('dashboard-tab');
    if (!root || typeof Chart === 'undefined') {
        return;
    }

    var apiUrl = root.getAttribute('data-dashboard-url') + '?period=' + (root.getAttribute('data-period') || '30d');

    Chart.defaults.font.family = "'Roboto', 'Helvetica Neue', Arial, sans-serif";
    Chart.defaults.color = '#6b7280';
    Chart.defaults.borderColor = '#e5e7eb';

    function buildLineChart(canvas, datasets, labels) {
        return new Chart(canvas, {
            type: 'line',
            data: { labels: labels, datasets: datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { intersect: false, mode: 'index' },
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 12, usePointStyle: true } } },
                scales: { y: { beginAtZero: true, grid: { drawBorder: false } }, x: { grid: { display: false } } }
            }
        });
    }

    function extractLabels(datasets) {
        var longest = [];
        for (var i = 0; i < datasets.length; i++) {
            if (datasets[i].length > longest.length) {
                longest = datasets[i];
            }
        }
        return longest.map(function (item) { return item.label; });
    }

    function lineData(items, label, color) {
        return {
            label: label,
            data: items.map(function (item) { return item.value; }),
            borderColor: color,
            backgroundColor: color + '14',
            borderWidth: 2,
            tension: 0.3,
            fill: true,
            pointRadius: 2,
            pointHoverRadius: 4
        };
    }

    function renderCharts(data) {
        var q = document.getElementById('questions-chart');
        if (q) {
            var qa = [data.questionsTrend || [], data.answersTrend || []];
            buildLineChart(q, [
                lineData((data.questionsTrend || []), 'Questions', '#1976d2'),
                lineData((data.answersTrend || []), 'Answers', '#00897b')
            ], extractLabels(qa));
        }

        var t = document.getElementById('traffic-chart');
        if (t) {
            buildLineChart(t, [lineData((data.trafficTrend || []), 'Visits', '#7e57c2')], extractLabels([data.trafficTrend || []]));
        }

        var r = document.getElementById('reputation-chart');
        if (r) {
            buildLineChart(r, [lineData((data.reputationTrend || []), 'Rep points', '#ef6c00')], extractLabels([data.reputationTrend || []]));
        }

        var topics = document.getElementById('topics-chart');
        if (topics && data.tags && data.tags.length) {
            var tags = data.tags.slice().sort(function (a, b) { return b.value - a.value; });
            new Chart(topics, {
                type: 'bar',
                data: { labels: tags.map(function (x) { return x.name; }),
                    datasets: [{ data: tags.map(function (x) { return x.value; }), backgroundColor: '#42a5f5', borderRadius: 4, maxBarThickness: 18 }] },
                options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { x: { beginAtZero: true, grid: { drawBorder: false } }, y: { grid: { display: false } } } }
            });
        }
    }

    fetch(apiUrl, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
        .then(function (res) { return res.ok ? res.json() : Promise.reject(res.status); })
        .then(renderCharts)
        .catch(function (err) {
            if (typeof console !== 'undefined') {
                console.error('Failed to load dashboard data', err);
            }
        });
}());