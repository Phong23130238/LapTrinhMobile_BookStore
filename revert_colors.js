const fs = require('fs');
const path = require('path');

const resDir = path.join(__dirname, 'app/src/main/res/layout');
const colorFile = path.join(__dirname, 'app/src/main/res/color/bottom_nav_selector.xml');

function replaceColorsInFile(filePath) {
    if (!fs.existsSync(filePath)) return;
    let content = fs.readFileSync(filePath, 'utf8');
    content = content.replace(/@color\/violet_primary/g, '@color/navy_dark');
    content = content.replace(/@color\/violet_dark/g, '@color/navy_medium');
    content = content.replace(/@color\/violet_light/g, '@color/navy_light');
    
    // Also replace hardcoded hex codes if any
    content = content.replace(/#8B5CF6/g, '@color/navy_dark');
    content = content.replace(/#6D28D9/g, '@color/navy_medium');
    content = content.replace(/#A78BFA/g, '@color/navy_light');
    content = content.replace(/#9333EA/g, '@color/navy_dark'); // For AI button

    fs.writeFileSync(filePath, content, 'utf8');
}

// Layout files
const files = fs.readdirSync(resDir);
files.forEach(f => {
    if (f.endsWith('.xml')) {
        replaceColorsInFile(path.join(resDir, f));
    }
});

// Color selector
replaceColorsInFile(colorFile);

// Themes
replaceColorsInFile(path.join(__dirname, 'app/src/main/res/values/themes.xml'));

console.log("Colors reverted!");
