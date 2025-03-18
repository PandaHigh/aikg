import sys
import os
import json
import argparse
import subprocess
import re
from typing import Dict, List, Optional, Tuple

def clean_html(html_content: str, preserve_content: bool = True) -> Tuple[str, Dict[str, int]]:
    """
    清理HTML内容，移除无用的标签和内容
    
    参数:
    - html_content: 原始HTML内容
    - preserve_content: 是否保留内容标签（如p、h1-h6、div等）
    
    返回:
    - 清理后的HTML内容和统计信息
    """
    if not html_content:
        return "", {"original_size": 0, "cleaned_size": 0}
    
    print("开始清理HTML内容...")
    original_size = len(html_content)
    stats = {
        "original_size": original_size,
        "script_tags": 0,
        "style_tags": 0,
        "comments": 0,
        "iframe_tags": 0,
        "noscript_tags": 0,
        "meta_tags": 0,
        "link_tags": 0,
        "empty_lines": 0,
        "js_events": 0,
        "data_attrs": 0
    }
    
    # 计数并移除script标签及其内容
    scripts = re.findall(r'<script[^>]*>[\s\S]*?</script>', html_content)
    stats["script_tags"] = len(scripts)
    html_content = re.sub(r'<script[^>]*>[\s\S]*?</script>', '', html_content)
    
    # 计数并移除style标签及其内容
    styles = re.findall(r'<style[^>]*>[\s\S]*?</style>', html_content)
    stats["style_tags"] = len(styles)
    html_content = re.sub(r'<style[^>]*>[\s\S]*?</style>', '', html_content)
    
    # 计数并移除注释
    comments = re.findall(r'<!--[\s\S]*?-->', html_content)
    stats["comments"] = len(comments)
    html_content = re.sub(r'<!--[\s\S]*?-->', '', html_content)
    
    # 计数并移除iframe标签
    iframes = re.findall(r'<iframe[^>]*>[\s\S]*?</iframe>', html_content)
    stats["iframe_tags"] = len(iframes)
    html_content = re.sub(r'<iframe[^>]*>[\s\S]*?</iframe>', '', html_content)
    
    # 计数并移除noscript标签
    noscripts = re.findall(r'<noscript[^>]*>[\s\S]*?</noscript>', html_content)
    stats["noscript_tags"] = len(noscripts)
    html_content = re.sub(r'<noscript[^>]*>[\s\S]*?</noscript>', '', html_content)
    
    # 移除meta标签
    meta_tags = re.findall(r'<meta[^>]*>', html_content)
    stats["meta_tags"] = len(meta_tags)
    html_content = re.sub(r'<meta[^>]*>', '', html_content)
    
    # 移除link标签
    link_tags = re.findall(r'<link[^>]*>', html_content)
    stats["link_tags"] = len(link_tags)
    html_content = re.sub(r'<link[^>]*>', '', html_content)
    
    # 移除所有JavaScript事件属性（如onclick, onload等）
    js_events = re.findall(r' on\w+="[^"]*"', html_content)
    stats["js_events"] = len(js_events)
    html_content = re.sub(r' on\w+="[^"]*"', '', html_content)
    
    # 移除所有data-*属性
    data_attrs = re.findall(r' data-\w+="[^"]*"', html_content)
    stats["data_attrs"] = len(data_attrs)
    html_content = re.sub(r' data-\w+="[^"]*"', '', html_content)
    
    # 移除所有class和id属性
    html_content = re.sub(r' class="[^"]*"', '', html_content)
    html_content = re.sub(r' id="[^"]*"', '', html_content)
    
    # 移除所有style属性
    html_content = re.sub(r' style="[^"]*"', '', html_content)
    
    # 如果不需要保留内容标签，则移除所有HTML标签，但保留其中的文本
    if not preserve_content:
        # 保存移除标签前的大小
        before_strip_tags = len(html_content)
        
        # 移除所有HTML标签，但保留其中的文本
        html_content = re.sub(r'<[^>]*>', ' ', html_content)
        
        # 记录移除的标签数量
        stats["removed_tags"] = before_strip_tags - len(html_content)
        print(f"移除了所有HTML标签，保留纯文本内容")
    else:
        # 只保留有用的内容标签，移除其他所有标签
        # 保留的标签：p, h1-h6, div, span, ul, ol, li, table, tr, td, th, a, strong, em, b, i
        content_before = len(html_content)
        
        # 创建一个临时文件来存储中间结果
        temp_content = html_content
        
        # 先提取所有我们想保留的标签内容
        preserved_content = []
        
        # 提取段落内容
        paragraphs = re.findall(r'<p[^>]*>([\s\S]*?)</p>', temp_content)
        preserved_content.extend(paragraphs)
        
        # 提取标题内容
        for i in range(1, 7):
            headings = re.findall(f'<h{i}[^>]*>([\s\S]*?)</h{i}>', temp_content)
            preserved_content.extend(headings)
        
        # 提取列表内容
        list_items = re.findall(r'<li[^>]*>([\s\S]*?)</li>', temp_content)
        preserved_content.extend(list_items)
        
        # 提取表格内容
        table_cells = re.findall(r'<t[dh][^>]*>([\s\S]*?)</t[dh]>', temp_content)
        preserved_content.extend(table_cells)
        
        # 提取div内容
        divs = re.findall(r'<div[^>]*>([\s\S]*?)</div>', temp_content)
        preserved_content.extend(divs)
        
        # 如果没有找到任何内容，则保留原始HTML
        if not preserved_content:
            print(f"没有找到有用的内容标签，保留原始HTML")
        else:
            # 将提取的内容合并成一个文档
            html_content = "\n\n".join(preserved_content)
            
            # 移除嵌套的HTML标签
            html_content = re.sub(r'<(?!/?(?:p|h[1-6]|ul|ol|li|table|tr|td|th|a|strong|em|b|i|div|span)\b)[^>]*>', '', html_content)
            
            print(f"保留了有用的内容标签，移除了 {content_before - len(html_content)} 字节的无用内容")
    
    # 移除多余的空白行并计数
    empty_lines = re.findall(r'\n\s*\n', html_content)
    stats["empty_lines"] = len(empty_lines)
    html_content = re.sub(r'\n\s*\n', '\n', html_content)
    
    # 移除多余的空格
    html_content = re.sub(r' {2,}', ' ', html_content)
    
    # 移除HTML实体编码
    html_content = re.sub(r'&nbsp;', ' ', html_content)
    html_content = re.sub(r'&lt;', '<', html_content)
    html_content = re.sub(r'&gt;', '>', html_content)
    html_content = re.sub(r'&amp;', '&', html_content)
    html_content = re.sub(r'&quot;', '"', html_content)
    
    # 计算清理后的大小
    cleaned_size = len(html_content)
    stats["cleaned_size"] = cleaned_size
    stats["reduction_percentage"] = round((original_size - cleaned_size) / original_size * 100, 2) if original_size > 0 else 0
    
    print(f"HTML内容清理完成，原始大小: {original_size} 字节，清理后: {cleaned_size} 字节，减少: {stats['reduction_percentage']}%")
    print(f"移除了 {stats['script_tags']} 个脚本标签, {stats['style_tags']} 个样式标签, {stats['comments']} 个注释")
    print(f"移除了 {stats['iframe_tags']} 个iframe标签, {stats['noscript_tags']} 个noscript标签")
    print(f"移除了 {stats['meta_tags']} 个meta标签, {stats['link_tags']} 个link标签, {stats['empty_lines']} 个空行")
    print(f"移除了 {stats['js_events']} 个JavaScript事件, {stats['data_attrs']} 个data属性")
    
    return html_content, stats

def get_html_content(url: str, preserve_content: bool = True) -> Optional[str]:
    """
    获取URL的HTML内容
    
    参数:
    - url: 要获取的URL
    - preserve_content: 是否保留内容标签
    
    返回:
    - 清理后的HTML内容
    """
    print(f"Python版本: {sys.version}")
    print(f"Python路径: {sys.executable}")
    print(f"当前工作目录: {os.getcwd()}")
    print(f"PYTHONPATH: {os.environ.get('PYTHONPATH', '')}")
    print(f"正在获取URL内容: {url}")
    
    # 直接使用curl获取内容
    cmd = [
        'curl', 
        '-k',                # 忽略SSL验证
        '-L',                # 跟随重定向
        '-A', "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        '-H', "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        '-H', "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8",
        url
    ]
    
    print(f"执行curl命令: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    
    if result.returncode == 0 and result.stdout:
        print(f"curl成功获取内容，状态码: {result.returncode}")
        # 清理HTML内容
        cleaned_html, stats = clean_html(result.stdout, preserve_content)
        return cleaned_html
    else:
        print(f"curl错误: {result.stderr}")
        print(f"curl状态码: {result.returncode}")
        return None

def main():
    parser = argparse.ArgumentParser(description='Web content fetcher')
    parser.add_argument('--url', required=True, help='URL to fetch')
    parser.add_argument('--output', required=True, help='Output file path')
    parser.add_argument('--strip-all-tags', action='store_true', help='Strip all HTML tags, keeping only text content')
    
    args = parser.parse_args()
    
    # 获取HTML内容
    html_content = get_html_content(args.url, not args.strip_all_tags)
    
    if html_content:
        # 保存HTML内容到文件
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(html_content)
        print(f"HTML内容已保存到: {args.output}")
        return 0
    else:
        print("获取HTML内容失败")
        return 1

if __name__ == "__main__":
    sys.exit(main()) 