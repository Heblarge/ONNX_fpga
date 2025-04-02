import os
from pathlib import Path

def generate_project_structure(
    root_dir: str = ".",
    output_file: str = "PROJECT_STRUCTURE.md",
    ignore_dirs: list = [".git", "__pycache__", "node_modules"],
    ignore_files: list = [".gitignore", "*.pyc"],
    max_depth: int = 5
):
    """
    生成项目目录结构文档
    :param root_dir: 项目根目录
    :param output_file: 输出文件名
    :param ignore_dirs: 要忽略的目录
    :param ignore_files: 要忽略的文件模式
    :param max_depth: 最大遍历深度
    """
    project_root = Path(root_dir)
    markdown = ["# 项目结构文档\n\n"]
    
    def should_ignore(path: Path) -> bool:
        if any(part in ignore_dirs for part in path.parts):
            return True
        if any(path.match(pattern) for pattern in ignore_files):
            return True
        return False
    
    def get_file_description(file_path: Path) -> str:
        """尝试从文件头部获取描述"""
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                first_line = f.readline().strip()
                if first_line.startswith("#"):
                    return first_line[1:].strip()
                if "Description:" in first_line:
                    return first_line.split("Description:")[1].strip()
        except:
            pass
        return "*(添加描述)*"
    
    def walk_dir(current_dir: Path, depth: int, parent_prefix: str = ""):
        if depth > max_depth:
            return
            
        entries = sorted(current_dir.iterdir(), key=lambda e: (not e.is_dir(), e.name))
        for index, entry in enumerate(entries):
            if should_ignore(entry):
                continue
                
            connector = "└── " if index == len(entries)-1 else "├── "
            prefix = "    " if index == len(entries)-1 else "│   "
            
            if entry.is_dir():
                markdown.append(f"{parent_prefix}{connector}📂 {entry.name}/\n")
                if depth < max_depth:
                    walk_dir(entry, depth+1, parent_prefix + prefix)
            else:
                description = get_file_description(entry)
                file_type = {
                            # 编程语言
                            ".py": "🐍",      # Python (保留蛇的图标)
                            ".scala": "🆂",
                            ".js": "𝐉𝐒",  # 粗体字母
                            ".ts": "🅃🅂",
                            ".java": "☕",    # Java (咖啡图标)
                            ".c": "🅒",       # C
                            ".cpp": "🅒🅟🅟", # C++
                            ".cs": "🅒#",     # C#
                            ".go": "🐹",      # Golang (土拨鼠)
                            ".rs": "🦀",      # Rust (螃蟹)
                            ".swift": "🐦",   # Swift (雨燕)
                            ".kt": "🅚🅣",    # Kotlin
                            
                            # 标记语言
                            ".html": "🌐",    # HTML (地球图标)
                            ".css": "🎨",     # CSS (调色板)
                            ".md": "📝",      # Markdown (文档)
                            
                            # 数据格式
                            ".json": "📦",    # JSON (保留包裹图标)
                            ".xml": "📡",     # XML (信号塔)
                            ".yaml": "⚙️",    # YAML (齿轮)
                            ".toml": "🔧",    # TOML (工具)
                            
                            # 配置文件
                            ".env": "🔑",     # 环境变量 (钥匙)
                            ".conf": "⚙️",    # 配置文件 (齿轮)
                            ".ini": "⚙️",
                            
                            # 文档
                            ".txt": "📄",     # 纯文本 (文档图标)
                            ".pdf": "📑",     # PDF
                            ".docx": "📃",    # Word
                            
                            # 构建工具
                            ".gradle": "🅶",  # Gradle
                            ".pom": "Ⓜ️",     # Maven
                }.get(entry.suffix, "📄")
                markdown.append(
                    f"{parent_prefix}{connector}{file_type} {entry.name}  \n"
                    f"{parent_prefix}{prefix}   // {description}\n"
                )
    
    walk_dir(project_root, 0)
    
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("".join(markdown))
        f.write("\n> 文档自动生成于 " + os.path.basename(root_dir))

if __name__ == "__main__":
    generate_project_structure(
        root_dir=".", 
        ignore_dirs=[".venv", "dist",".bloop",".bsp",".idea",".vscode",".git",".metals","target","tmp"],
        ignore_files=["*.tmp"]
    )