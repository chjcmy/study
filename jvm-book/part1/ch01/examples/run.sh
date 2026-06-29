#!/usr/bin/env bash
# ch01 Kotlin 예시 실행 스크립트
# 실행 권한: chmod +x run.sh
# 전제: kotlinc, java 21+ PATH에 있어야 함

set -e
cd "$(dirname "$0")"

run_script() {
    local name=$1
    echo "[script] $name.kts"
    kotlinc -script "${name}.kts"
}

run_jvminfo() {
    echo -e "\n========== JvmInfo ==========\n"
    run_script JvmInfo
}

run_bytecode() {
    echo -e "\n========== BytecodeTarget ==========\n"
    run_script BytecodeTarget
    echo ""
    echo "바이트코드 확인은 BytecodeTarget.kts 상단 주석의 javap 절차를 참고하세요."
}

run_classloader() {
    echo -e "\n========== ClassLoaderHierarchy ==========\n"
    run_script ClassLoaderHierarchy
}

run_jit() {
    echo -e "\n========== JitWarmupDemo ==========\n"
    run_script JitWarmupDemo
}

run_vthread() {
    echo -e "\n========== VirtualThreadDemo ==========\n"
    run_script VirtualThreadDemo
}

run_graal() {
    echo -e "\n========== GraalNativeHint ==========\n"
    run_script GraalNativeHint
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
