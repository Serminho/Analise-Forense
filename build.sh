#!/bin/bash

# Configuração
PROJECT_DIR="$(pwd)"
SRC_DIR="src"
BUILD_DIR="build"
LIB_DIR="lib"
JAR_NAME="analise-forense-aed.jar"

# Limpar build anterior
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Verificar se Java está disponível
if ! command -v javac &> /dev/null; then
    echo "❌ javac não encontrado. Instale o JDK!"
    exit 1
fi

if ! command -v jar &> /dev/null; then
    echo "❌ jar não encontrado. Instale o JDK!"
    exit 1
fi

echo "☕ Usando Java:"
java -version

# Compilar código fonte
if [ ! -d "$SRC_DIR" ]; then
    echo "❌ Diretório src/ não encontrado!"
    exit 1
fi

echo "📦 Compilando arquivos .java..."

javac -d "$BUILD_DIR" -cp "$LIB_DIR/*" $(find "$SRC_DIR" -name "*.java")

if [ $? -ne 0 ]; then
    echo "❌ Falha na compilação!"
    exit 1
fi

# Verificar classes compiladas
find "$BUILD_DIR" -name "*.class" | while read class_file; do
    class_name=$(echo "$class_file" | sed "s|$BUILD_DIR/||" | sed 's|/|.|g' | sed 's|.class||')
    echo "  🎯 $class_name"
done

# Criar JAR

cd "$BUILD_DIR"
jar cf "../$JAR_NAME" .
cd "$PROJECT_DIR"

echo "➕ Incorporando analise-forense-aed.jar ao JAR final..."
cd "$LIB_DIR"
jar xf analise-forense-aed.jar
cd "$PROJECT_DIR"

cd "$BUILD_DIR"
jar uf "../$JAR_NAME" br
cd "$PROJECT_DIR"

ls -lh "$JAR_NAME"
jar tf "$JAR_NAME" | head -10
if [ $(jar tf "$JAR_NAME" | wc -l) -gt 10 ]; then
    echo "  ... e mais $(( $(jar tf "$JAR_NAME" | wc -l) - 10 )) arquivos"
fi

# Verificar se JAR é válido
if jar tf "$JAR_NAME" > /dev/null 2>&1; then
    echo "✅ JAR válido!"
else
    echo "❌ JAR inválido!"
    exit 1
fi

