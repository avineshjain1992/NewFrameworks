#!/usr/bin/env bash
# Linux cloud bootstrap. General Windows/macOS setup is documented in each README.
set -euo pipefail
TASK_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export TASK_ROOT
python3 - <<'PY'
import hashlib, os, pathlib, shlex, shutil, tarfile, urllib.parse, urllib.request
import xml.etree.ElementTree as ET
root=pathlib.Path(os.environ['TASK_ROOT'])
local=root/'.local';local.mkdir(exist_ok=True)
cache=pathlib.Path('/workspace/.cache');cache.mkdir(exist_ok=True)
tools=pathlib.Path('/workspace/tools');tools.mkdir(exist_ok=True)
jdk=tools/'jdk-21.0.10+7'
if not (jdk/'bin/javac').exists():
    url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.10%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.10_7.tar.gz'
    expected=urllib.request.urlopen(url+'.sha256.txt').read().decode().split()[0]
    data=urllib.request.urlopen(url).read()
    if hashlib.sha256(data).hexdigest()!=expected: raise RuntimeError('JDK checksum verification failed')
    archive=cache/'jdk.tar.gz';archive.write_bytes(data)
    with tarfile.open(archive) as tar:tar.extractall(tools,filter='data')
settings=ET.Element('settings',xmlns='http://maven.apache.org/SETTINGS/1.2.0')
u=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
if u.hostname:
    proxy=ET.SubElement(ET.SubElement(settings,'proxies'),'proxy')
    for key,value in [('id','cloud'),('active','true'),('protocol','http'),('host',u.hostname),('port',str(u.port or 80)),('nonProxyHosts','localhost|127.*')]:ET.SubElement(proxy,key).text=value
    if u.username or u.password:raise RuntimeError('Authenticated proxy requires a platform-managed Maven settings binding; no credentials are copied')
path=local/'maven-settings.xml';ET.ElementTree(settings).write(path)
variables={'JAVA_HOME':str(jdk),'MAVEN_USER_HOME':str(cache/'maven'),'MAVEN_ARGS':f'-s {path} -Dmaven.repo.local={cache}/m2','MAVEN_OPTS':'-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts','npm_config_cache':str(cache/'npm'),'npm_config_update_notifier':'false','PLAYWRIGHT_BROWSERS_PATH':str(cache/'ms-playwright'),'PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD':'1'}
chromium=shutil.which('chromium')
if chromium:variables['BROWSER_EXECUTABLE_PATH']=chromium
(local/'cloud-env.sh').write_text(''.join(f'export {key}={shlex.quote(value)}\n' for key,value in variables.items()))
print('Cloud initialization written without credentials.')
PY
source "$TASK_ROOT/.local/cloud-env.sh"
cd "$TASK_ROOT"
npm --prefix playwright-javascript ci
./playwright-java/mvnw -B -ntp -f playwright-java/pom.xml install -DskipTests spotless:check
./restassured-java/mvnw -B -ntp -f restassured-java/pom.xml test-compile spotless:check
# Warm the clean goal used by the managed runners for offline repeatability.
./playwright-java/mvnw -B -ntp -f playwright-java/pom.xml clean test-compile
./restassured-java/mvnw -B -ntp -f restassured-java/pom.xml clean test-compile
if [[ -z "${BROWSER_EXECUTABLE_PATH:-}" ]]; then
  (cd playwright-javascript && npx playwright install chromium)
  ./playwright-java/mvnw -B -ntp -f playwright-java/web-tests/pom.xml org.codehaus.mojo:exec-maven-plugin:3.6.4:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test '-Dexec.args=install chromium'
else
  echo 'Using explicit system Chromium fallback. Pinned browsers and FFmpeg still need browser-download access.'
fi
