#!groovy

import com.e4d.mysql.MySqlDbName

def call(
    overriden_parameters,
    file2override = null,
    placeholder = null,
    value = null,
    template = null
)
{
    file2override = file2override ?: 'secret.json'
    placeholder = placeholder ?: '__DBNAMEPLACEHOLDER__'
    template = template ?: GLOBAL_AURORADB_TEMPLATE
    value = value ?: new MySqlDbName(env.JOB_NAME, "${env.JOB_BASE_NAME}-${env.BUILD_ID}").toString()

    def value2override = "$template".replace("$placeholder", "$value")
    overriden_parameters = overriden_parameters + "'" + value2override + "'"

    return overriden_parameters 
}
