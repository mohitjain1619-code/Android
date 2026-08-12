const fs = require('fs');
const path = require('path');

function searchDir(dir) {
    const files = fs.readdirSync(dir);
    files.forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            if (file !== 'node_modules' && file !== '.git' && file !== 'build') {
                searchDir(fullPath);
            }
        } else if (file.endsWith('.java')) {
            const content = fs.readFileSync(fullPath, 'utf8');
            if (content.includes('applyWindowInsets')) {
                console.log(`Found call in: ${fullPath}`);
            }
        }
    });
}

searchDir('c:/Users/mohit/Documents/AndroidStudioProjects - V15/camverz');
