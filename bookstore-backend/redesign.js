const fs = require('fs');
const path = require('path');

const rootDir = path.join(__dirname, '..', 'app', 'src', 'main', 'res');
const layoutDir = path.join(rootDir, 'layout');
const drawableDir = path.join(rootDir, 'drawable');

function replaceInFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    
    // Background replacements
    content = content.replace(/android:background="@color\/navy_dark"/g, 'android:background="@color/bg_light"');
    
    // Text Color replacements
    content = content.replace(/android:textColor="@color\/cream_dark"/g, 'android:textColor="@color/charcoal_medium"');
    content = content.replace(/android:textColor="@color\/gold"/g, 'android:textColor="@color/violet_primary"');
    content = content.replace(/android:textColor="@color\/navy_dark"/g, 'android:textColor="@color/charcoal_dark"');
    content = content.replace(/android:textColor="@color\/navy_medium"/g, 'android:textColor="@color/violet_primary"');
    content = content.replace(/android:textColor="@color\/navy_light"/g, 'android:textColor="@color/violet_light"');
    
    // General color replacements (for tints, backgrounds of small elements)
    content = content.replace(/@color\/navy_dark/g, '@color/violet_primary');
    content = content.replace(/@color\/navy_medium/g, '@color/violet_light');
    content = content.replace(/@color\/gold/g, '@color/peach_accent');

    // Corners
    content = content.replace(/app:cardCornerRadius="(8|10|12|16|20)dp"/g, 'app:cardCornerRadius="28dp"');
    content = content.replace(/app:cornerRadius="(8|10|12)dp"/g, 'app:cornerRadius="28dp"');

    fs.writeFileSync(filePath, content, 'utf8');
}

function processDirectory(dir) {
    if (!fs.existsSync(dir)) return;
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fullPath.endsWith('.xml')) {
            replaceInFile(fullPath);
        }
    }
}

processDirectory(layoutDir);
processDirectory(drawableDir);
console.log("Redesign complete!");
