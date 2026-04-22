#!/usr/bin/env bash
# ch01 Kotlin 예시 실행 스크립트
# 실행 권한: chmod +x run.sh
# 전제: kotlinc, java 21+ PATH에 있어야 함

set -e
cd "$(dirname "$0")"

compile() {
    local name=$1
    echo "[compile] $name.kt"
    kotlinc "${name}.kt" -include-runtime -d "${name}.jar" 2>/dev/null
}

run_jvminfo() {
    echo -e "\n========== JvmInfo ==========\n"
    compile JvmInfo
    java -jar JvmInfo.jar
}

run_bytecode() {
    echo -e "\n========== BytecodeTarget — javap ==========\n"
    compile BytecodeTarget
    jar xf BytecodeTarget.jar
    echo "--- data class Item ---"
    javap -p Item.class 2>/dev/null || echo "(Item.class not found)"
    echo ""
    echo "--- sealed class Status\$Active ---"
    javap -p 'Status$Active.class' 2>/dev/null || echo "(not found)"
    echo ""
    echo "--- top-level fun → BytecodeTargetKt ---"
    javap -c BytecodeTargetKt.class 2>/dev/null || echo "(not found)"
}

run_classloader() {
    echo -e "\n========== ClassLoaderHierarchy ==========\n"
    compile ClassLoaderHierarchy
    java -jar ClassLoaderHierarchy.jar
}

run_jit() {
    compile JitWarmupDemo
    echo -e "\n========== JitWarmupDemo — JIT ON ==========\n"
    java -jar JitWarmupDemo.jar

    echo -e "\n========== JitWarmupDemo — -Xint (JIT OFF) ==========\n"
    java -Xint -jar JitWarmupDemo.jar
}

run_vthread() {
    echo -e "\n========== VirtualThreadDemo ==========\n"
    compile VirtualThreadDemo
    java -jar VirtualThreadDemo.jar
}

run_graal() {
    echo -e "\n========== GraalNativeHint ==========\n"
    compile GraalNativeHint
    echo "--- JVM 실행 ---"
    time java -jar GraalNativeHint.jar
}

case "${1:-all}" in
    jvminfo)     run_jvminfo ;;
    bytecode)    run_bytecode ;;
    classloader) run_classloader ;;
    jit)         run_jit ;;
    vthread)     run_vthread ;;
    graal)       run_graal ;;
    all)
        run_jvminfo
        run_classloader
        run_jit
        run_vthread
        run_graal
        ;;
    *)
        echo "usage: ./run.sh [jvminfo|bytecode|classloader|jit|vthread|graal|all]"
        ;;
esac
