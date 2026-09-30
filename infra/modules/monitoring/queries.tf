# Saved Logs Insights queries, only when the environment has a log group to point them at
# (a null entry in log_group_names is rejected at plan time)

resource "aws_cloudwatch_query_definition" "errors_by_logger" {
  count = local.has_log_group ? 1 : 0

  name            = "${var.name}/errors-by-logger"
  log_group_names = [local.log_group_name]

  query_string = <<-QUERY
    fields @timestamp, @message, logger_name, message
    | filter level = "ERROR" or @message like /ERROR/
    | stats count(*) as errors by logger_name
    | sort errors desc
  QUERY
}

resource "aws_cloudwatch_query_definition" "fail_open_warnings_over_time" {
  count = local.has_log_group ? 1 : 0

  name            = "${var.name}/fail-open-warnings-over-time"
  log_group_names = [local.log_group_name]

  query_string = <<-QUERY
    fields @timestamp, @message
    | filter @message like /Rate limiter unavailable|Cache read failed/
    | stats count(*) as warnings by bin(1m)
    | sort @timestamp asc
  QUERY
}

resource "aws_cloudwatch_query_definition" "slowest_requests" {
  count = local.has_log_group ? 1 : 0

  name            = "${var.name}/slowest-requests"
  log_group_names = [local.log_group_name]

  query_string = <<-QUERY
    fields @timestamp, @message, duration_ms, uri, status
    | filter ispresent(duration_ms)
    | sort duration_ms desc
    | limit 50
  QUERY
}
