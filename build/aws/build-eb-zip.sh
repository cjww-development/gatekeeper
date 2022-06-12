#
# Copyright 2022 CJWW Development
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

VERSION=$1
ACC_ID=$2

mkdir gatekeeper

cp Dockerrun.aws.json gatekeeper

sed -i "" -e "s/gatekeeper.*\"/gatekeeper:${VERSION}\"/" gatekeeper/Dockerrun.aws.json
sed -i "" -e "s/account/${ACC_ID}/g" gatekeeper/Dockerrun.aws.json

cd gatekeeper
zip -r ../gatekeeper-${VERSION}.zip .
cd ..

rm -rf gatekeeper