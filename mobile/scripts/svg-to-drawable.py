#!/bin/env python
import math
import re
import sys
import xml.etree.ElementTree as ET

if (len(sys.argv) < 3):
    sys.exit(1)

input_file = sys.argv[1]
output_file = sys.argv[2]

ANDROID_NS = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID_NS)


def local_name(tag):
    return tag.rsplit("}", 1)[-1]


def android_attr(name):
    return f"{{{ANDROID_NS}}}{name}"


def parse_style(value):
    result = {}
    for item in (value or "").split(";"):
        if ":" not in item:
            continue
        key, val = item.split(":", 1)
        result[key.strip()] = val.strip()
    return result


def merged_style(node):
    style = parse_style(node.attrib.get("style"))
    for key in (
        "fill",
        "fill-opacity",
        "stroke",
        "stroke-opacity",
        "stroke-width",
        "stroke-linecap",
        "stroke-linejoin",
        "stroke-miterlimit",
    ):
        if key in node.attrib:
            style[key] = node.attrib[key]
    return style


def parse_number(value, default=None):
    if value is None:
        return default
    match = re.search(r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?", value)
    if not match:
        return default
    return float(match.group(0))


def format_number(value):
    if abs(value) < 0.000001:
        value = 0.0
    text = f"{value:.6f}".rstrip("0").rstrip(".")
    return text or "0"


def parse_view_box(root):
    raw = root.attrib.get("viewBox")
    if raw:
        values = [float(part) for part in re.split(r"[\s,]+", raw.strip()) if part]
        if len(values) == 4:
            return values
    width = parse_number(root.attrib.get("width"), 24.0)
    height = parse_number(root.attrib.get("height"), 24.0)
    return [0.0, 0.0, width, height]


def color_value(value, opacity=None):
    value = (value or "").strip()
    if value in ("", "none"):
        return "@android:color/transparent"
    if value.startswith("rgb("):
        nums = [int(float(part)) for part in re.findall(r"[-+]?\d+(?:\.\d+)?", value)[:3]]
        if len(nums) == 3:
            value = "#{:02X}{:02X}{:02X}".format(*nums)
    if value.startswith("#"):
        hex_part = value[1:]
        if len(hex_part) == 3:
            hex_part = "".join(ch * 2 for ch in hex_part)
        if opacity not in (None, "", "1", "1.0"):
            alpha = max(0, min(255, round(float(opacity) * 255)))
            return f"#{alpha:02X}{hex_part.upper()}"
        return f"#{hex_part.upper()}"
    return value


def apply_paint_attrs(out, style):
    fill = style.get("fill", "#000000")
    stroke = style.get("stroke")
    out.set(android_attr("fillColor"), color_value(fill, style.get("fill-opacity")))
    if stroke and stroke != "none":
        out.set(android_attr("strokeColor"), color_value(stroke, style.get("stroke-opacity")))
    if "stroke-width" in style:
        out.set(android_attr("strokeWidth"), format_number(parse_number(style["stroke-width"], 0.0)))
    if "stroke-linecap" in style:
        out.set(android_attr("strokeLineCap"), style["stroke-linecap"])
    if "stroke-linejoin" in style:
        out.set(android_attr("strokeLineJoin"), style["stroke-linejoin"])
    if "stroke-miterlimit" in style:
        out.set(android_attr("strokeMiterLimit"), format_number(parse_number(style["stroke-miterlimit"], 4.0)))


def circle_path(cx, cy, r):
    left = cx - r
    right = cx + r
    return (
        f"M{format_number(left)},{format_number(cy)}"
        f"A{format_number(r)},{format_number(r)} 0,1 0,{format_number(right)} {format_number(cy)}"
        f"A{format_number(r)},{format_number(r)} 0,1 0,{format_number(left)} {format_number(cy)}"
    )


def ellipse_path(cx, cy, rx, ry):
    left = cx - rx
    right = cx + rx
    return (
        f"M{format_number(left)},{format_number(cy)}"
        f"A{format_number(rx)},{format_number(ry)} 0,1 0,{format_number(right)} {format_number(cy)}"
        f"A{format_number(rx)},{format_number(ry)} 0,1 0,{format_number(left)} {format_number(cy)}"
    )


def rect_path(node):
    x = parse_number(node.attrib.get("x"), 0.0)
    y = parse_number(node.attrib.get("y"), 0.0)
    width = parse_number(node.attrib.get("width"), 0.0)
    height = parse_number(node.attrib.get("height"), 0.0)
    return (
        f"M{format_number(x)},{format_number(y)}"
        f"H{format_number(x + width)}"
        f"V{format_number(y + height)}"
        f"H{format_number(x)}Z"
    )


def poly_path(node, close):
    points = re.findall(
        r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?",
        node.attrib.get("points", ""),
    )
    coords = [float(value) for value in points]
    pairs = list(zip(coords[0::2], coords[1::2]))
    if not pairs:
        return ""
    head = pairs[0]
    tail = pairs[1:]
    data = [f"M{format_number(head[0])},{format_number(head[1])}"]
    data.extend(f"L{format_number(x)},{format_number(y)}" for x, y in tail)
    if close:
        data.append("Z")
    return "".join(data)


def parse_transform(value):
    attrs = {}
    if not value:
        return attrs
    for name, args in re.findall(r"([a-zA-Z]+)\(([^)]*)\)", value):
        values = [parse_number(part, 0.0) for part in re.split(r"[\s,]+", args.strip()) if part]
        if name == "translate" and values:
            attrs[android_attr("translateX")] = format_number(values[0])
            attrs[android_attr("translateY")] = format_number(values[1] if len(values) > 1 else 0.0)
        elif name == "scale" and values:
            attrs[android_attr("scaleX")] = format_number(values[0])
            attrs[android_attr("scaleY")] = format_number(values[1] if len(values) > 1 else values[0])
        elif name == "rotate" and values:
            attrs[android_attr("rotation")] = format_number(values[0])
            if len(values) >= 3:
                attrs[android_attr("pivotX")] = format_number(values[1])
                attrs[android_attr("pivotY")] = format_number(values[2])
        else:
            print(f"warning: unsupported transform skipped: {name}({args})", file=sys.stderr)
    return attrs


def append_path(parent, node, path_data):
    if not path_data:
        return
    out = ET.SubElement(parent, "path")
    out.set(android_attr("pathData"), path_data)
    apply_paint_attrs(out, merged_style(node))


def convert_children(in_parent, out_parent):
    for child in list(in_parent):
        tag = local_name(child.tag)
        if tag in ("defs", "metadata", "namedview"):
            continue
        transform_attrs = parse_transform(child.attrib.get("transform"))
        target_parent = out_parent
        if tag == "g" or transform_attrs:
            group = ET.SubElement(out_parent, "group")
            for key, value in transform_attrs.items():
                group.set(key, value)
            target_parent = group
        if tag == "g":
            convert_children(child, target_parent)
        elif tag == "path":
            append_path(target_parent, child, child.attrib.get("d", ""))
        elif tag == "circle":
            cx = parse_number(child.attrib.get("cx"), 0.0)
            cy = parse_number(child.attrib.get("cy"), 0.0)
            r = parse_number(child.attrib.get("r"), 0.0)
            append_path(target_parent, child, circle_path(cx, cy, r))
        elif tag == "ellipse":
            cx = parse_number(child.attrib.get("cx"), 0.0)
            cy = parse_number(child.attrib.get("cy"), 0.0)
            rx = parse_number(child.attrib.get("rx"), 0.0)
            ry = parse_number(child.attrib.get("ry"), 0.0)
            append_path(target_parent, child, ellipse_path(cx, cy, rx, ry))
        elif tag == "rect":
            append_path(target_parent, child, rect_path(child))
        elif tag == "line":
            x1 = parse_number(child.attrib.get("x1"), 0.0)
            y1 = parse_number(child.attrib.get("y1"), 0.0)
            x2 = parse_number(child.attrib.get("x2"), 0.0)
            y2 = parse_number(child.attrib.get("y2"), 0.0)
            append_path(target_parent, child, f"M{format_number(x1)},{format_number(y1)}L{format_number(x2)},{format_number(y2)}")
        elif tag == "polyline":
            append_path(target_parent, child, poly_path(child, close=False))
        elif tag == "polygon":
            append_path(target_parent, child, poly_path(child, close=True))
        else:
            print(f"warning: unsupported SVG element skipped: {tag}", file=sys.stderr)


tree = ET.parse(input_file)
root = tree.getroot()
min_x, min_y, viewport_width, viewport_height = parse_view_box(root)

vector = ET.Element("vector")
vector.set(android_attr("width"), "24dp")
vector.set(android_attr("height"), "24dp")
vector.set(android_attr("viewportWidth"), format_number(viewport_width))
vector.set(android_attr("viewportHeight"), format_number(viewport_height))

content_parent = vector
if min_x or min_y:
    content_parent = ET.SubElement(vector, "group")
    content_parent.set(android_attr("translateX"), format_number(-min_x))
    content_parent.set(android_attr("translateY"), format_number(-min_y))

convert_children(root, content_parent)

ET.indent(vector, space="    ")
ET.ElementTree(vector).write(output_file, encoding="utf-8", xml_declaration=True)
