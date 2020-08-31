import os

dirPath = os.path.dirname(os.path.realpath(__file__))
targetSuffix = "_ENG"


def main():
    print("自动搜索文件并互换文件名。例如如果有个xxx.csv，相同目录下有对应的xxx_suffix.csv，那会互换两者的文件名。")
    print("Auto-search files and swap file names if there is a file and a file which has the same former name + suffix.")
    print("Root:" + dirPath)
    for root, subfolders, filenames in os.walk(dirPath):
        changed = []
        for filename in filenames:
            filePath = os.path.join(root, filename)
            if filePath in changed:
                continue
            (shortname, extension) = os.path.splitext(filename)
            replacePath = os.path.join(root, shortname + targetSuffix + extension)
            if(os.path.isfile(replacePath)):
                changed.append(filePath)
                print(filePath.replace(dirPath, "...") + " --> " +
                      replacePath.replace(dirPath, "..."))
                tempPath = os.path.join(root, shortname + "temp" + extension)
                try:
                    os.rename(replacePath, tempPath)
                    os.rename(filePath, replacePath)
                    os.rename(tempPath, filePath)
                except Exception as e:
                    print(e)
                    print('rename file fail\r\n')
    input("Press Any key To Exit.")


if __name__ == '__main__':
    main()
