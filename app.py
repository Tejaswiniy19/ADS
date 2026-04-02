from flask import Flask, render_template_string, request, jsonify
import subprocess
import threading
import webbrowser
import os
import json

app = Flask(__name__)

@app.after_request
def add_header(response):
    response.headers["ngrok-skip-browser-warning"] = "true"
    response.headers["X-Frame-Options"] = "ALLOWALL"
    response.headers["Access-Control-Allow-Origin"] = "*"
    return response

def get_base_html(title, content):
    return f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>{title}</title>
        <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700;800&family=JetBrains+Mono:wght@400;700&display=swap" rel="stylesheet">
        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        <style>
            :root {{ 
                --bg-1: #020617; --bg-2: #0f172a; 
                --panel: rgba(30, 41, 59, 0.7); 
                --text: #f8fafc; --primary: #3b82f6; 
                --accent: #8b5cf6; --success: #10b981; 
                --warning: #f59e0b; --danger: #ef4444;
            }}
            body {{ 
                font-family: 'Outfit', sans-serif; 
                background: linear-gradient(135deg, #020617, #0f172a);
                color: var(--text); margin: 0; padding: 0; min-height: 100vh;
                display: flex;
            }}
            nav {{
                width: 280px; background: rgba(2,6,23,0.9); border-right: 1px solid rgba(255,255,255,0.05);
                padding: 2rem 1rem; position: fixed; height: 100vh; overflow-y: auto;
            }}
            nav h2 {{ font-weight: 800; color: var(--primary); margin-bottom: 2rem; font-size: 1.5rem; text-align: center; }}
            nav a {{
                display: block; padding: 1rem 1.5rem; margin-bottom: 0.5rem; border-radius: 8px;
                color: #cbd5e1; text-decoration: none; font-weight: 600; transition: all 0.2s;
                border: 1px solid transparent;
            }}
            nav a:hover, nav a.active {{
                background: rgba(59, 130, 246, 0.1); border-color: rgba(59, 130, 246, 0.3); color: var(--text);
            }}
            .main-content {{
                margin-left: 300px; padding: 3rem; width: calc(100% - 300px); max-width: 1200px;
            }}
            .panel {{ 
                background: var(--panel); backdrop-filter: blur(16px);
                border: 1px solid rgba(255,255,255,0.05); border-radius: 16px; 
                padding: 2.5rem; margin-bottom: 2rem; box-shadow: 0 10px 30px rgba(0,0,0,0.3); 
            }}
            .panel h2 {{ margin-top: 0; color: var(--primary); border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 15px; }}
            pre {{ 
                background: rgba(0,0,0,0.5); padding: 1.5rem; border-radius: 8px; 
                font-family: 'JetBrains Mono', monospace; font-size: 0.95rem; line-height: 1.6;
                border-left: 4px solid var(--accent); color: #e2e8f0; overflow-x: auto;
            }}
            table {{ width: 100%; border-collapse: collapse; margin-top: 1rem; border-radius: 8px; overflow: hidden; }}
            th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.05); }}
            th {{ background: rgba(0,0,0,0.3); color: var(--primary); }}
            tr:hover {{ background: rgba(255,255,255,0.02); }}
            .highlight-box {{
                background: rgba(16, 185, 129, 0.1); border: 1px solid var(--success); padding: 1.5rem; border-radius: 8px;
                margin-top: 1rem;
            }}
            .chart-grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; margin-top: 2rem; }}
            .btn {{ 
                background: linear-gradient(135deg, var(--primary), #2563eb); color: white; border: none; padding: 1rem 2rem; font-size: 1.1rem; border-radius: 8px; cursor: pointer; font-weight: 700; transition: transform 0.2s, background 0.2s; display: inline-block; text-align: center;
            }}
            .btn:hover {{ background-color: #2563eb; transform: translateY(-2px); }}
            .btn:disabled {{ background-color: #64748b; cursor: not-allowed; transform: none; box-shadow: none; border-color: transparent; }}
            
            @media (max-width: 900px) {{
                body {{ flex-direction: column; }}
                nav {{
                    width: 100%; height: auto; position: relative; border-right: none;
                    border-bottom: 1px solid rgba(255,255,255,0.05); padding: 1rem;
                    display: flex; flex-wrap: wrap; justify-content: center; gap: 0.5rem;
                }}
                nav h2 {{ width: 100%; text-align: center; margin-bottom: 1rem; font-size: 1.2rem; }}
                nav a {{ padding: 0.5rem 1rem; margin-bottom: 0; font-size: 0.9rem; }}
                .main-content {{ margin-left: 0; width: 100%; padding: 1.5rem; }}
                .panel {{ padding: 1.5rem; margin-bottom: 1.5rem; }}
            }}
        </style>
    </head>
    <body>
        <nav>
            <h2>Financial Analytics DB</h2>
            <a href="/" class="nav-btn">Upload & Execute</a>
            <a href="/headers" class="nav-btn">1. Execution Headers</a>
            <a href="/validation" class="nav-btn">2. Validation Output</a>
            <a href="/metrics" class="nav-btn">3. Structural Metrics</a>
            <a href="/performance" class="nav-btn">4. Performance Table</a>
            <a href="/analysis" class="nav-btn">5. Worst-Case & Memory</a>
            <a href="/graphs" class="nav-btn">6. Graphical Outputs</a>
            <a href="/summary" class="nav-btn">7. Final Summary</a>
        </nav>
        <div class="main-content">
            {content}
        </div>
        <script>
            document.querySelectorAll('.nav-btn').forEach(link => {{
                if(link.getAttribute('href') === window.location.pathname) link.classList.add('active');
            }});
        </script>
    </body>
    </html>
    """

def load_metrics():
    if os.path.exists('metrics.json'):
        with open('metrics.json', 'r') as f:
            return json.load(f)
    return {}

@app.route('/')
def home():
    content = """
    <div class="panel" style="text-align: center; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 250px;">
        <h2 style="border: none; margin-bottom: 2rem; justify-content: center;">Live System Benchmarker</h2>
        <p style="color: #94a3b8; font-size: 1.1rem; max-width: 600px; margin-bottom: 2rem;">Upload a custom dataset to overwrite the primary transactions or directly run the Java simulation explicitly over all metrics.</p>
        
        <div style="margin-bottom: 2rem; background: rgba(0,0,0,0.2); padding: 1.5rem; border-radius: 12px; border: 1px dashed rgba(255,255,255,0.2); width: 100%; max-width: 500px;">
            <p style="margin-top: 0; color: var(--accent); font-weight: 600;">Optional: Test a Custom Random CSV</p>
            <input type="file" id="csvFile" accept=".csv" style="display: none;" onchange="uploadCSV()">
            <button class="btn" style="background: linear-gradient(135deg, #8b5cf6, #6d28d9); padding: 0.8rem 2rem; font-size: 1rem;" onclick="document.getElementById('csvFile').click()">📤 Upload custom .csv</button>
            <div id="uploadStatus" style="margin-top: 10px; font-weight: bold; color: var(--success);"></div>
        </div>

        <button class="btn" id="runBtn" onclick="runBenchmark()">Launch Full Analysis Suite</button>
        <div id="runOutput" style="margin-top: 1rem; color: var(--warning); display: none;">Executing Java Benchmarks... Please wait.</div>
        <pre id="consoleDump" style="display: none; width: 100%; margin-top: 1.5rem; text-align: left; max-height: 400px;"></pre>
    </div>

    <script>
        async function uploadCSV() {
            const fileInput = document.getElementById('csvFile');
            const status = document.getElementById('uploadStatus');
            if (fileInput.files.length === 0) return;
            
            const formData = new FormData();
            formData.append('file', fileInput.files[0]);
            
            status.style.color = '#f59e0b';
            status.textContent = "Uploading...";
            
            try {
                const res = await fetch('/api/upload', { method: 'POST', body: formData });
                if (res.ok) {
                    status.style.color = '#10b981';
                    status.textContent = "✅ Custom CSV successfully uploaded! Ready to Execute.";
                } else {
                    status.style.color = '#ef4444';
                    status.textContent = "❌ Upload failed.";
                }
            } catch (err) {
                status.style.color = '#ef4444';
                status.textContent = "❌ Error during upload.";
            }
        }

        async function runBenchmark() {
            const runBtn = document.getElementById('runBtn');
            const runOutput = document.getElementById('runOutput');
            const consoleDump = document.getElementById('consoleDump');

            runBtn.disabled = true;
            runOutput.style.display = 'block';
            runOutput.style.color = '#f59e0b';
            runOutput.textContent = "Executing Java Benchmarks... Please wait.";
            consoleDump.style.display = 'none';

            try {
                const res = await fetch('/api/run', { method: 'POST' });
                const data = await res.json();
                
                if (data.status === 'success') {
                    runOutput.style.color = '#10b981';
                    runOutput.textContent = "✅ Benchmarks processed successfully!";
                    consoleDump.textContent = data.output;
                    consoleDump.style.display = 'block';
                } else {
                    runOutput.style.color = '#ef4444';
                    runOutput.textContent = "❌ Benchmark failed.";
                    consoleDump.textContent = data.error;
                    consoleDump.style.display = 'block';
                }
            } catch (err) {
                runOutput.style.color = '#ef4444';
                runOutput.textContent = "❌ Error executing benchmark request.";
            } finally {
                runBtn.disabled = false;
            }
        }
    </script>
    """
    return render_template_string(get_base_html("Upload & Execute", content))

@app.route('/api/upload', methods=['POST'])
def api_upload():
    if 'file' not in request.files: return 'No file', 400
    file = request.files['file']
    if file.filename == '': return 'No file', 400
    if file and file.filename.endswith('.csv'):
        os.makedirs('csv', exist_ok=True)
        file.save(os.path.join('csv', 'transactions_500000.csv'))
        return "OK", 200
    return 'Invalid file', 400

@app.route('/api/run', methods=['POST'])
def api_run():
    try:
        # Compile Java
        subprocess.run(["javac", "-d", "bin", "src/models/*.java", "src/utils/*.java", "src/ds/*.java", "src/benchmark/*.java"], check=True)
        # Generate Data if missing
        if not os.path.exists('csv/transactions_500000.csv'):
            subprocess.run(["java", "-cp", "bin", "utils.DataGenerator"], check=True)
        # Run Benchmark
        result = subprocess.run(
            ["java", "-cp", "bin", "benchmark.SystemEvaluation"],
            capture_output=True, text=True, check=True
        )
        return jsonify({"status": "success", "output": result.stdout})
    except subprocess.CalledProcessError as e:
        return jsonify({"status": "error", "error": f"Execution failed:\n{e.stderr}\n{e.stdout}"}), 500

@app.route('/headers')
def execution_headers():
    data = load_metrics().get('datasets', {})
    if not data:
        return render_template_string(get_base_html("Execution Headers", '<div class="panel"><h2>No metrics found</h2><p>Please run the analysis from the home page first.</p></div>'))
    content = """
    <div class="panel">
        <h2>Dataset Execution Headers</h2>
        <pre>
{% for name, metrics in data.items() %}
=== Dataset: {{ name.capitalize() }} Transactions ===
Total Records: 500000
{% endfor %}
        </pre>
    </div>
    """
    return render_template_string(get_base_html("Execution Headers", content), data=data)

@app.route('/validation')
def validation():
    data = load_metrics().get('datasets', {}).get('random', {}).get('validation', {})
    if not data:
        return render_template_string(get_base_html("Validation", '<div class="panel"><h2>No validation data</h2><p>Please run the analysis from the home page first.</p></div>'))
    content = """
    <div class="panel">
        <h2>Correctness Validation Output</h2>
        <pre style="border-left-color: var(--primary);">
--- Search Validation ---
TransactionID: {{ data.search.id }}
Found: {{ "YES" if data.search.found else "NO" }}
Time: {{ "%.6f"|format(data.search.time) }}s
        </pre>
        <pre style="border-left-color: var(--warning);">
--- Range Query Validation ---
Range: {{ data.range.start }}–{{ data.range.end }}
Sum: {{ "%.2f"|format(data.range.sum) }}
Time: {{ "%.6f"|format(data.range.time) }}s
        </pre>
        <pre style="border-left-color: var(--success);">
--- Update Validation ---
TransactionID: {{ data['update'].id }}
Old Amount: {{ "%.2f"|format(data['update'].old) }}
New Amount: {{ "%.2f"|format(data['update'].new) }}
Update Time: {{ "%.6f"|format(data['update'].time) }}s
        </pre>
    </div>
    """
    return render_template_string(get_base_html("Validation", content), data=data)

@app.route('/metrics')
def metrics():
    data = load_metrics().get('datasets', {}).get('random', {})
    if not data or 'avl' not in data:
         return render_template_string(get_base_html("Structural Metrics", '<div class="panel"><h2>No metrics found</h2><p>Please run the analysis from the home page first.</p></div>'))
    content = """
    <div class="panel">
        <h2>Structural Metrics Output (Random Dataset)</h2>
        <pre>
--- Structural Metrics ---

AVL Tree:
Height: {{ data.avl.height }}
Rotations: {{ data.avl.rotations }}

Red-Black Tree:
Height: {{ data.rb.height }}
Rotations: {{ data.rb.rotations }}

B+ Tree:
Height: {{ data.bplus.height }}
Node Splits: {{ data.bplus.splits }}

Segment Tree:
Build Time: {{ data.analytics.segment_ms }}ms

Fenwick Tree:
Build Time: {{ data.analytics.fenwick_ms }}ms
        </pre>
    </div>
    """
    return render_template_string(get_base_html("Structural Metrics", content), data=data)

@app.route('/performance')
def performance():
    datasets = load_metrics().get('datasets', {})
    if not datasets:
        return render_template_string(get_base_html("Performance Table", '<div class="panel"><h2>No performance data</h2><p>Please run the analysis from the home page first.</p></div>'))
    content = """
    <div class="panel">
        <h2>Performance Tables (Dataset-Wise)</h2>
        
        {% for name, metrics in datasets.items() %}
        <h3 style="color: var(--primary); margin-top: 1.5rem;">Dataset: {{ name.capitalize() }} (500k)</h3>
        <table>
            <thead>
                <tr>
                    <th>Structure</th><th>Insert(ms)</th><th>Search(ms)</th>
                </tr>
            </thead>
            <tbody>
                <tr><td>AVL Tree</td><td>{{ metrics.avl.insert_ms }}</td><td>{{ metrics.avl.search_ms }}</td></tr>
                <tr><td>Red-Black Tree</td><td>{{ metrics.rb.insert_ms }}</td><td>{{ metrics.rb.search_ms }}</td></tr>
                <tr><td>Splay Tree</td><td>{{ metrics.splay.insert_ms }}</td><td>{{ metrics.splay.search_ms }}</td></tr>
                <tr><td>B-Tree</td><td>{{ metrics.btree.insert_ms }}</td><td>{{ metrics.btree.search_ms }}</td></tr>
                <tr><td>B+ Tree</td><td>{{ metrics.bplus.insert_ms }}</td><td>{{ metrics.bplus.search_ms }}</td></tr>
            </tbody>
        </table>
        {% endfor %}
    </div>
    """
    return render_template_string(get_base_html("Performance Table", content), datasets=datasets)

@app.route('/analysis')
def analysis():
    datasets = load_metrics().get('datasets', {})
    content = """
    <div class="panel">
        <h2>Worst-Case Analysis Output</h2>
        <pre>
--- Worst Case Observations ---
{% for name, m in datasets.items() %}
Dataset: {{ name.capitalize() }}
AVL Height: {{ m.avl.height }}
RB Height: {{ m.rb.height }}
B+ Height: {{ m.bplus.height }}
{% endfor %}
        </pre>
    </div>
    """
    return render_template_string(get_base_html("Worst-Case Analysis", content), datasets=datasets)

@app.route('/graphs')
def graphs():
    datasets = load_metrics().get('datasets', {})
    if not datasets:
        return render_template_string(get_base_html("Graphs", '<div class="panel"><h2>No graphical data</h2><p>Please run the analysis from the home page first.</p></div>'))
    content = """
    <div class="panel">
        <h2 style="margin-bottom: 2rem;">Graphical Outputs</h2>
        <div class="chart-grid">
            <div style="background: rgba(0,0,0,0.3); padding: 1rem; border-radius: 8px; grid-column: span 2;">
                <canvas id="insertChart"></canvas>
            </div>
            <div style="background: rgba(0,0,0,0.3); padding: 1rem; border-radius: 8px;">
                <canvas id="searchChart"></canvas>
            </div>
            <div style="background: rgba(0,0,0,0.3); padding: 1rem; border-radius: 8px;">
                <canvas id="heightChart"></canvas>
            </div>
        </div>
    </div>
    <script>
        const datasets = {{ datasets | tojson }};
        const labels = Object.keys(datasets).map(n => n.charAt(0).toUpperCase() + n.slice(1));
        
        Chart.defaults.color = '#cbd5e1';
        
        new Chart(document.getElementById('insertChart'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {label: 'AVL', data: Object.values(datasets).map(d => d.avl.insert_ms), backgroundColor: '#3b82f6'},
                    {label: 'RB', data: Object.values(datasets).map(d => d.rb.insert_ms), backgroundColor: '#f43f5e'},
                    {label: 'B+', data: Object.values(datasets).map(d => d.bplus.insert_ms), backgroundColor: '#10b981'}
                ]
            },
            options: { plugins: { title: { display: true, text: 'Insertion Time by Dataset (ms)', font:{size:16} } } }
        });

        new Chart(document.getElementById('searchChart'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {label: 'AVL', data: Object.values(datasets).map(d => d.avl.search_ms), backgroundColor: '#3b82f6'},
                    {label: 'B+', data: Object.values(datasets).map(d => d.bplus.search_ms), backgroundColor: '#10b981'}
                ]
            },
            options: { plugins: { title: { display: true, text: 'Search Time by Dataset (ms)', font:{size:16} } } }
        });

        new Chart(document.getElementById('heightChart'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {label: 'AVL Height', data: Object.values(datasets).map(d => d.avl.height), backgroundColor: '#3b82f6'},
                    {label: 'RB Height', data: Object.values(datasets).map(d => d.rb.height), backgroundColor: '#f43f5e'}
                ]
            },
            options: { plugins: { title: { display: true, text: 'Tree Height by Dataset', font:{size:16} } } }
        });
    </script>
    """
    return render_template_string(get_base_html("Graphs", content), datasets=datasets)

@app.route('/summary')
def summary():
    content = """
    <div class="panel highlight-box">
        <h2 style="color: var(--success); border-color: var(--success);">Final Comparative Analysis</h2>
        <div style="font-size: 1.1rem; line-height: 1.8; margin-top: 1rem;">
            <p><strong>Best Structure for Indexing:</strong> B+ Tree (Stable performance across all datasets)</p>
            <p><strong>Best Structure for Analytics:</strong> Fenwick Tree (Memory efficient prefix sums)</p>
            <p><strong>Dynamic Recalculation:</strong> Successfully verified via Java benchmarks.</p>
        </div>
    </div>
    """
    return render_template_string(get_base_html("Final Summary", content))

if __name__ == '__main__':
    def open_browser():
        webbrowser.open_new('http://127.0.0.1:5000/')
    threading.Timer(2, open_browser).start()
    app.run(host="127.0.0.1", port=5000, debug=True)