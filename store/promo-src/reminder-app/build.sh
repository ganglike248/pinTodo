# 작은 '리마인더' 앱 빌드·설치 (Gradle 없이 SDK 도구로)
set -e; R=$(cd "$(dirname "$0")" && pwd); O=$R/../work/remind; rm -rf $O; mkdir -p $O
BT=$(ls -d ~/Library/Android/sdk/build-tools/* | tail -1); PJ=$(ls -d ~/Library/Android/sdk/platforms/android-3* | head -1)/android.jar
$BT/aapt2 compile --dir $R/res -o $O/res.zip
$BT/aapt2 link -I $PJ --manifest $R/AndroidManifest.xml -o $O/base.apk $O/res.zip
javac -nowarn -source 11 -target 11 -cp $PJ -d $O/classes $R/src/com/demo/remind/*.java 2>/dev/null
$BT/d8 --lib $PJ --output $O $O/classes/com/demo/remind/*.class
cp $O/base.apk $O/u.apk; (cd $O && zip -q u.apk classes.dex)
$BT/zipalign -f 4 $O/u.apk $O/a.apk
$BT/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --out $O/remind.apk $O/a.apk
adb -e install -r --no-incremental $O/remind.apk && adb -e shell pm grant com.demo.remind android.permission.POST_NOTIFICATIONS
