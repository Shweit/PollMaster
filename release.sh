#!/bin/bash

# PollMaster Release Script
# Usage: ./release.sh 1.0.4

if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.4"
    exit 1
fi

VERSION=$1

echo "🚀 Starting release process for version $VERSION..."

# 1. Update version in pom.xml
echo "📝 Updating version in pom.xml..."
mvn versions:set -DnewVersion=$VERSION -q

# 2. Clean and package
echo "🔨 Building project..."
mvn clean package -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # 3. Git operations
    echo "📦 Creating git commit and tag..."
    git add .
    git commit -m "Release v$VERSION" -q
    git tag v$VERSION
    
    echo "🎉 Release v$VERSION completed!"
    echo "📁 JAR file: target/PollMaster-$VERSION.jar"
    echo ""
    echo "To push to remote:"
    echo "git push origin main --tags"
else
    echo "❌ Build failed!"
    exit 1
fi
