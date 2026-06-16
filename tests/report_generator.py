import os
import datetime
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from openpyxl.utils import get_column_letter

class TestReporter:
    def __init__(self):
        # Configure output paths
        self.base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.results_dir = os.path.join(self.base_dir, "Test Results")
        
        self.excel_dir = os.path.join(self.results_dir, "Excel")
        self.html_dir = os.path.join(self.results_dir, "HTML")
        self.summary_dir = os.path.join(self.results_dir, "Summary")
        
        os.makedirs(self.excel_dir, exist_ok=True)
        os.makedirs(self.html_dir, exist_ok=True)
        os.makedirs(self.summary_dir, exist_ok=True)
        
        self.excel_path = os.path.join(self.excel_dir, "Automation_Test_Report.xlsx")
        self.html_path = os.path.join(self.html_dir, "execution-report.html")
        self.summary_path = os.path.join(self.summary_dir, "summary.md")

    def generate_reports(self, steps, is_success):
        self.generate_excel(steps, is_success)
        self.generate_html(steps, is_success)
        self.generate_summary(steps, is_success)

    def generate_excel(self, steps, is_success):
        wb = Workbook()
        
        # 1. Summary Sheet
        ws_summary = wb.active
        ws_summary.title = "Execution Summary"
        ws_summary.views.sheetView[0].showGridLines = True
        
        # Colors
        fill_header = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
        fill_sub_header = PatternFill(start_color="DCE6F1", end_color="DCE6F1", fill_type="solid")
        fill_pass = PatternFill(start_color="E2EFDA", end_color="E2EFDA", fill_type="solid")
        fill_fail = PatternFill(start_color="FCE4D6", end_color="FCE4D6", fill_type="solid")
        
        font_header = Font(name="Calibri", size=14, bold=True, color="FFFFFF")
        font_bold = Font(name="Calibri", size=11, bold=True)
        font_normal = Font(name="Calibri", size=11)
        font_pass = Font(name="Calibri", size=11, bold=True, color="385723")
        font_fail = Font(name="Calibri", size=11, bold=True, color="C00000")
        
        border_thin = Side(border_style="thin", color="D9D9D9")
        border_double = Side(border_style="double", color="333333")
        cell_border = Border(left=border_thin, right=border_thin, top=border_thin, bottom=border_thin)
        bottom_double_border = Border(left=border_thin, right=border_thin, top=border_thin, bottom=border_double)
        
        # Add summary content
        ws_summary.merge_cells("A1:D1")
        ws_summary["A1"] = "Automation Test Execution Summary"
        ws_summary["A1"].font = font_header
        ws_summary["A1"].fill = fill_header
        ws_summary["A1"].alignment = Alignment(horizontal="center", vertical="center")
        
        ws_summary.row_dimensions[1].height = 40
        
        ws_summary.append([]) # empty row
        
        ws_summary.append(["Attribute", "Value"])
        ws_summary["A3"].font = font_bold
        ws_summary["A3"].fill = fill_sub_header
        ws_summary["B3"].font = font_bold
        ws_summary["B3"].fill = fill_sub_header
        
        total_steps = len(steps)
        passed_steps = sum(1 for s in steps if s[1] == "Passed")
        failed_steps = total_steps - passed_steps
        pass_rate = (passed_steps / total_steps * 100) if total_steps > 0 else 0.0
        
        ws_summary.append(["Suite Name", "Smart Civic Governance E2E Flow"])
        ws_summary.append(["Execution Date", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
        ws_summary.append(["Total Steps", total_steps])
        ws_summary.append(["Passed Steps", passed_steps])
        ws_summary.append(["Failed Steps", failed_steps])
        ws_summary.append(["Execution Status", "PASSED" if is_success else "FAILED"])
        
        # Apply fonts and alignments to summary details
        for row in range(4, 10):
            ws_summary[f"A{row}"].font = font_bold
            ws_summary[f"A{row}"].border = cell_border
            ws_summary[f"B{row}"].font = font_normal
            ws_summary[f"B{row}"].border = cell_border
            
        # Highlight Status
        status_cell = ws_summary["B9"]
        status_cell.font = font_pass if is_success else font_fail
        status_cell.fill = fill_pass if is_success else fill_fail

        # Auto-adjust columns width
        for col in ws_summary.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws_summary.column_dimensions[col_letter].width = max(max_len + 3, 15)

        # 2. Steps Sheet
        ws_steps = wb.create_sheet(title="Step Details")
        ws_steps.views.sheetView[0].showGridLines = True
        
        headers = ["Step Number & Name", "Status", "Details/Logs", "Timestamp"]
        ws_steps.append(headers)
        ws_steps.row_dimensions[1].height = 25
        
        for col_idx, h in enumerate(headers, 1):
            cell = ws_steps.cell(row=1, column=col_idx)
            cell.font = font_bold
            cell.fill = fill_sub_header
            cell.alignment = Alignment(vertical="center")
            cell.border = cell_border
            
        now_str = datetime.datetime.now().strftime("%H:%M:%S")
        for step_idx, step in enumerate(steps, 1):
            ws_steps.append([step[0], step[1], step[2], now_str])
            row_idx = step_idx + 1
            ws_steps.row_dimensions[row_idx].height = 20
            
            # Format cells
            c0 = ws_steps.cell(row=row_idx, column=1)
            c1 = ws_steps.cell(row=row_idx, column=2)
            c2 = ws_steps.cell(row=row_idx, column=3)
            c3 = ws_steps.cell(row=row_idx, column=4)
            
            c0.font = font_bold
            c0.border = cell_border
            
            c1.font = font_pass if step[1] == "Passed" else font_fail
            c1.fill = fill_pass if step[1] == "Passed" else fill_fail
            c1.alignment = Alignment(horizontal="center")
            c1.border = cell_border
            
            c2.font = font_normal
            c2.border = cell_border
            
            c3.font = font_normal
            c3.alignment = Alignment(horizontal="center")
            c3.border = cell_border

        # Adjust columns width for steps sheet
        for col in ws_steps.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws_steps.column_dimensions[col_letter].width = max(max_len + 3, 15)
            
        wb.save(self.excel_path)

    def generate_html(self, steps, is_success):
        total_steps = len(steps)
        passed_steps = sum(1 for s in steps if s[1] == "Passed")
        failed_steps = total_steps - passed_steps
        pass_rate = round((passed_steps / total_steps * 100), 1) if total_steps > 0 else 0.0
        
        status_color = "#10b981" if is_success else "#ef4444"
        status_text = "PASSED" if is_success else "FAILED"
        date_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        steps_rows_html = ""
        for idx, step in enumerate(steps, 1):
            badge_class = "badge-pass" if step[1] == "Passed" else "badge-fail"
            steps_rows_html += f"""
            <tr>
                <td>{idx}</td>
                <td class="step-name">{step[0]}</td>
                <td><span class="badge {badge_class}">{step[1]}</span></td>
                <td>{step[2]}</td>
            </tr>
            """

        html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Automation Execution Report</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        body {{
            background-color: #0b0f19;
            color: #f8fafc;
            font-family: 'Outfit', sans-serif;
            margin: 0;
            padding: 2rem;
        }}
        .container {{
            max-width: 1100px;
            margin: 0 auto;
        }}
        .header {{
            background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
            border: 1px solid rgba(255,255,255,0.06);
            border-radius: 12px;
            padding: 1.5rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2rem;
            box-shadow: 0 4px 20px rgba(0,0,0,0.3);
        }}
        .header h1 {{
            margin: 0;
            font-size: 1.75rem;
            font-weight: 700;
            background: linear-gradient(to right, #ffffff, #cbd5e1);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }}
        .header p {{
            margin: 0.25rem 0 0 0;
            color: #94a3b8;
            font-size: 0.9rem;
        }}
        .status-badge {{
            background-color: {status_color};
            color: white;
            font-weight: 700;
            font-size: 1.1rem;
            padding: 0.4rem 1.2rem;
            border-radius: 50px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            box-shadow: 0 0 15px {status_color}50;
        }}
        .stats-grid {{
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 1.5rem;
            margin-bottom: 2.5rem;
        }}
        .stat-card {{
            background: rgba(30, 41, 59, 0.4);
            border: 1px solid rgba(255,255,255,0.06);
            border-radius: 12px;
            padding: 1.25rem;
            text-align: center;
        }}
        .stat-card p {{
            margin: 0 0 0.25rem 0;
            color: #94a3b8;
            font-size: 0.85rem;
            text-transform: uppercase;
            font-weight: 600;
            letter-spacing: 0.5px;
        }}
        .stat-card h2 {{
            margin: 0;
            font-size: 2rem;
            font-weight: 700;
        }}
        .glass-card {{
            background: rgba(30, 41, 59, 0.25);
            backdrop-filter: blur(12px);
            border: 1px solid rgba(255,255,255,0.06);
            border-radius: 12px;
            padding: 1.5rem;
            box-shadow: 0 8px 30px rgba(0,0,0,0.25);
        }}
        .glass-card h3 {{
            margin: 0 0 1.25rem 0;
            font-size: 1.2rem;
            border-bottom: 1px solid rgba(255,255,255,0.06);
            padding-bottom: 0.5rem;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }}
        th {{
            background-color: rgba(255,255,255,0.03);
            color: white;
            padding: 0.75rem 1rem;
            font-weight: 600;
            font-size: 0.85rem;
            border-bottom: 1px solid rgba(255,255,255,0.06);
        }}
        td {{
            padding: 0.75rem 1rem;
            font-size: 0.9rem;
            border-bottom: 1px solid rgba(255,255,255,0.04);
            color: #cbd5e1;
        }}
        tr:hover td {{
            background-color: rgba(255,255,255,0.01);
            color: white;
        }}
        .step-name {{
            font-weight: 600;
        }}
        .badge {{
            padding: 0.2rem 0.6rem;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: 700;
            text-transform: uppercase;
        }}
        .badge-pass {{
            background-color: rgba(16, 185, 129, 0.15);
            color: #10b981;
        }}
        .badge-fail {{
            background-color: rgba(239, 68, 68, 0.15);
            color: #ef4444;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div>
                <h1>E2E Test Execution Report</h1>
                <p>Suite: Smart Civic Governance Portal | Date: {date_str}</p>
            </div>
            <div class="status-badge">{status_text}</div>
        </div>
        
        <div class="stats-grid">
            <div class="stat-card">
                <p>Total Steps</p>
                <h2>{total_steps}</h2>
            </div>
            <div class="stat-card" style="color: #10b981;">
                <p>Passed Steps</p>
                <h2>{passed_steps}</h2>
            </div>
            <div class="stat-card" style="color: #ef4444;">
                <p>Failed Steps</p>
                <h2>{failed_steps}</h2>
            </div>
            <div class="stat-card" style="color: #3b82f6;">
                <p>Pass Percentage</p>
                <h2>{pass_rate}%</h2>
            </div>
        </div>
        
        <div class="glass-card">
            <h3>Detailed Step Execution Log</h3>
            <table>
                <thead>
                    <tr>
                        <th style="width: 80px;">Step</th>
                        <th>Step Description</th>
                        <th style="width: 100px;">Status</th>
                        <th>Detailed Log Output / Failure Message</th>
                    </tr>
                </thead>
                <tbody>
                    {steps_rows_html}
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>"""

        with open(self.html_path, "w", encoding="utf-8") as f:
            f.write(html_content)

    def generate_summary(self, steps, is_success):
        total_steps = len(steps)
        passed_steps = sum(1 for s in steps if s[1] == "Passed")
        failed_steps = total_steps - passed_steps
        pass_rate = f"{round((passed_steps / total_steps * 100), 1)}%" if total_steps > 0 else "0%"
        
        # Read repo/run info from GitHub actions env if available
        repo_name = os.environ.get("GITHUB_REPOSITORY", "<github-username>/<repository-name>")
        github_username = repo_name.split("/")[0] if "/" in repo_name else "<github-username>"
        repository_name = repo_name.split("/")[1] if "/" in repo_name else "<repository-name>"
        
        deployment_url = f"https://{github_username}.github.io/{repository_name}/"

        failed_section = ""
        if failed_steps > 0:
            failed_section = "### Failed Steps:\n"
            for step in steps:
                if step[1] == "Failed":
                    failed_section += f"- **{step[0]}**\n  - *Reason:* {step[2]}\n"
        else:
            failed_section = "### Failed Steps:\n- None. All steps passed cleanly!"

        markdown = f"""# Live GitHub Pages E2E Test Summary

**Deployment URL:**
{deployment_url}

### Key Metrics:
- **Total Tests/Steps:** {total_steps}
- **Passed:** {passed_steps}
- **Failed:** {failed_steps}
- **Skipped:** 0
- **Pass Percentage:** {pass_rate}

{failed_section}
"""
        with open(self.summary_path, "w", encoding="utf-8") as f:
            f.write(markdown)
