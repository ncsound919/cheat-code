import os

print("cwd:", os.getcwd())
print("\n/kaggle exists:", os.path.exists("/kaggle"))
for root, dirs, files in os.walk("/kaggle/input"):
    depth = root.count("/") - 2
    print(f"{'  ' * depth}{root}")
    if depth >= 2:
        dirs[:] = []
        continue
    for f in files[:8]:
        p = os.path.join(root, f)
        print(f"{'  ' * (depth + 1)}- {f} ({os.path.getsize(p):,} bytes)")
