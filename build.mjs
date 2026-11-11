import fs from 'node:fs';
fs.writeFileSync('dist/app.js',['src/core.js','src/modules.js','src/input-updates.js','src/dashboard.js','src/fund-information.js','src/ui.js','src/distribution-profile.js','src/distribution-packages.js','src/login.js'].map(p=>fs.readFileSync(p,'utf8')).join('\n'));
