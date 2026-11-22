"""Extract communes and urban centres from the HCP RGPH 2024 workbook (stdlib only)."""
import io
import json
from pathlib import Path
import re
from urllib.request import urlopen
from zipfile import ZipFile
import xml.etree.ElementTree as ET

SOURCE = 'https://www.hcp.ma/file/242341/'
NS = {'s': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}

with urlopen(SOURCE, timeout=45) as response, ZipFile(io.BytesIO(response.read())) as archive:
    strings = [''.join(item.itertext()) for item in
               ET.fromstring(archive.read('xl/sharedStrings.xml')).findall('s:si', NS)]
    rows = ET.fromstring(archive.read('xl/worksheets/sheet1.xml')).findall('s:sheetData/s:row', NS)
    locations = {}
    province = ''
    for row in rows:
        cells = {}
        for cell in row:
            value = cell.find('s:v', NS)
            text = value.text if value is not None else ''
            cells[re.sub(r'\d', '', cell.get('r'))] = strings[int(text)] if cell.get('t') == 's' else text
        label = cells.get('A', '').strip()
        if label.startswith(('Province ', 'Préfecture ')):
            province = re.sub(r"^(Province|Préfecture) (de |d['’])", '', label)
        if not label.startswith(('Commune ', 'dont le centre urbain ')):
            continue
        name = re.sub(r"^(Commune |dont le centre urbain )(de |d['’])", '', label).rstrip('*').strip()
        key = (name.casefold(), province)
        if key not in locations:
            locations[key] = dict(id=cells['G'], name=name, province=province,
                                  arabic=cells.get('F', '').rstrip('*').strip())

assert len(locations) > 1500, 'Unexpected HCP workbook structure; review before replacing data.'
output = Path(__file__).resolve().parents[1] / 'src/data/moroccan-locations.json'
output.parent.mkdir(exist_ok=True)
output.write_text(json.dumps(sorted(locations.values(), key=lambda item: item['name'].casefold()),
                             ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
print(f'{len(locations)} distinct communes and urban centres written to {output.name}.')
