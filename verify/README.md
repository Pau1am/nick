# verify/

`VerifyCommandTree.java` 检查一个**没有安装本模组**的客户端能否正确解析服务端下发的 `/nick` 命令树。

背景：服务端会把整棵命令树发给每个客户端。如果树里出现模组自定义的参数类型，客户端按数字 id 查表会拿到 `null`，该参数节点会退化成 `RootCommandNode`，而它的父节点会**静默跳过**它——结果是 `/nick set` 分支整段从客户端命令树里消失（补全、语法提示都没了；不过客户端仍然会照常把 `/nick set ...` 发出去，所以命令本身还能用）。这类问题不会报错、也不会掉线，因此很容易被忽略，需要专门检查。

## 这个脚本做了什么

1. 调用模组**自身的注册代码** `NickCommand.registerCommands`，构造真实的命令树。
2. 用原版 `ClientboundCommandsPacket` 编码器序列化——和服务端实际发送的一致。
3. 用原版解码器解析回来——客户端的第一步。
4. 反射检查每个参数节点是否有 `null` stub（这正是客户端无法解析时的表现）。
5. 调用 `getRoot(buildContext, nodeBuilder)` 走一遍客户端重建命令树的代码，并确认 `nick -> set -> name` 这条链确实存在。
6. **反向对照**：手写一段含「客户端不认识的参数类型」的数据包，断言它**会被检出**。没有这一步，前面的检查无法证明有效。

## 怎么跑

需要一份 Loom 环境的运行时 classpath。最简单的方式是先跑一次服务端，然后从生成的参数文件里导出：

```bash
# 1. 生成（并顺带验证）服务端能启动
./gradlew runServer

# 2. 从 Loom 的启动参数里导出 classpath（引号是 Gradle 的转义，去掉即可）
python - <<'PY'
import os
src = open("build/loom-cache/argFiles/runServer", encoding="utf-8").read()
entries = [e.strip() for e in src.split("-classpath", 1)[1].replace('"', "").split(";")]
keep = []
for e in entries:
    if e.startswith("-"):
        break
    keep.append(e)
open("verify/classpath.txt", "w", encoding="utf-8").write(os.pathsep.join(keep))
PY

# 3. 编译并运行
CP=$(cat verify/classpath.txt)
javac -nowarn -cp "$CP" -d verify/classes verify/VerifyCommandTree.java
java -cp "verify/classes;$CP" VerifyCommandTree
```

以 `RESULT: PASS` 结束即通过。`verify/classes/`、`verify/classpath.txt` 与 `verify/result-*.txt` 都是本地产物，已在 `.gitignore` 中忽略。
