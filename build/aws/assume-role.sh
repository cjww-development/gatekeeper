#!/usr/bin/env sh

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

set +x
SESSIONID=$(date +"%s")

RESULT=($(aws sts assume-role --role-arn "$ROLE_ARN" \
        --role-session-name "$SESSIONID" \
        --query '[Credentials.AccessKeyId,Credentials.SecretAccessKey,Credentials.SessionToken]' \
        --output text))

export AWS_ACCESS_KEY_ID=${RESULT[0]}
export AWS_SECRET_ACCESS_KEY=${RESULT[1]}
export AWS_SECURITY_TOKEN=${RESULT[2]}
export AWS_SESSION_TOKEN=${AWS_SECURITY_TOKEN}
set -x
