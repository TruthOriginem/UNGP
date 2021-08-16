# Coded by Originem
# All Rights Reserved
import os
import zipfile
from datetime import datetime

# 现在时间
now = datetime.now()
# 工作目录
cwd = os.getcwd()
# 上一级文件夹->一般是mods文件夹，压缩包最终输出到该文件夹
mods_folder = os.path.dirname(cwd)
# 文件夹名字(mod名字)
mod_name = os.path.basename(cwd)
# 版本名字
version = "095Dev"
# 输出名字
output_name = "{0}-{1}-{2}".format(mod_name, version,
                                   now.strftime("%y%m%d-%H%M%S"))


exclude_prefixes = ('__', '.')  # exclusion prefixes
exclude_suffixes = ('.lnk', '.iml', '.py')


def zipdir(path, ziph):
    # ziph is zipfile handle
    for root, dirs, files in os.walk(path):
        # ignore the . hidden files
        files = [f for f in files if not f.startswith(
            exclude_prefixes) and not f.endswith(exclude_suffixes)]
        dirs[:] = [d for d in dirs if not d.startswith(
            exclude_prefixes) and not d.endswith(exclude_suffixes) and not d == 'src']
        for file in files:
            ziph.write(os.path.join(root, file),
                       os.path.relpath(os.path.join(root, file),
                                       os.path.join(path, '..')))


print("Start compressing!")
zipf = zipfile.ZipFile('../{0}.zip'.format(output_name),
                       'w', zipfile.ZIP_DEFLATED)
zipdir(cwd, zipf)
zipf.close()
print("Compress Completed!")
