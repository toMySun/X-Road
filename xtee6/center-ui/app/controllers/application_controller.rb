require 'json'
require 'common-ui/uploaded_file.rb'

java_import Java::ee.ria.xroad.common.AuditLogger
java_import Java::ee.ria.xroad.common.SystemProperties

class ApplicationController < BaseController
  include MembersHelper

  FEDERATION_PRIVILEGES = [
    :view_external_configuration_source,
    :view_trusted_anchors,
    :upload_trusted_anchor,
    :delete_trusted_anchor,
    :download_trusted_anchor
  ]

  around_filter :wrap_in_transaction

  rescue_from ActiveRecord::StatementInvalid,
      ActiveRecord::ConnectionNotEstablished do
    begin
      ActiveRecord::Base.establish_connection
    rescue
      log_stacktrace($!)
      render_error_response(t("common.db_error"))
    end
  end

  before_filter :disable_federation
  before_filter :read_locale
  before_filter :check_conf, :except => [:menu]
  before_filter :read_server_id, :read_ha_node_name, :except => [:menu]
  before_filter :verify_get, :only => [
      :index,
      :get_records_count,
      :can_see_details]

  def member_classes
    render_json(MemberClass.get_all_codes)
  end

  # Number of total entries in the list
  def get_records_count
    raise "This must be overridden by explicit controller!"
  end

  def set_locale
    audit_log("Set UI language", audit_log_params = {})

    validate_params({
      :locale => [:required]
    })

    unless I18n.available_locales.include?(params[:locale].to_sym)
      raise "invalid locale"
    end

    audit_log_params[:locale] = params[:locale]

    ui_user = UiUser.find_by_username(current_user.name)

    unless ui_user
      ui_user = UiUser.new
      ui_user.username = current_user.name
    end

    ui_user.locale = params[:locale]
    ui_user.save!

    render :nothing => true
  end

  private

  def check_conf
    redirect_to :controller => :init unless initialized?
  end

  def initialized?
    SystemParameter.instance_identifier &&
      SystemParameter.central_server_address &&
      software_token_initialized?
  end

  def render(*args)
    if is_postgres?
      unless ActiveRecord::Base.connection.outside_transaction?
        ActiveRecord::Base.connection.commit_db_transaction
      end
    end

    execute_after_commit_actions

    # Everything that can fail has been done,
    # now let's do the actual rendering.
    super
  end

  def wrap_in_transaction
    ActiveRecord::Base.isolation_level(:repeatable_read) do
      ActiveRecord::Base.transaction do
        set_transaction_variables
        yield
      end
    end
  end

  def is_postgres?
    ActiveRecord::Base.connection.adapter_name == "PostgreSQL"
  end

  # Passes the required variables to the database engine if supported.
  def set_transaction_variables
    if is_postgres?
      # If we are running on top of Postgres, the name of the logged-in
      # user must be made available within the transaction, for use
      # when updating the history table.
      # The value of user_name will go out of scope when the transaction
      # ends.
      statement = "SET LOCAL xroad.user_name='#{current_user.name}'"
      ActiveRecord::Base.connection.execute(statement)
    end
  end

  def get_sort_column_no
    params[:iSortCol_0].to_i
  end

  def get_list_query_params(sort_column)
    ListQueryParams.new(
      sort_column,
      params[:sSortDir_0],
      params[:iDisplayStart],
      params[:iDisplayLength],
      params[:sSearch])
  end

  def get_advanced_search_params(request_param)
    return nil unless request_param

    raw_params = JSON.parse(request_param)

    return AdvancedSearchParams.new(
      {
        :name => raw_params["name"],
        :xroad_instance => raw_params["xRoadInstance"],
        :member_class => raw_params["memberClass"],
        :member_code => raw_params["memberCode"],
        :subsystem_code => raw_params["subsystem"],
        :object_type => raw_params["objectType"],
        :service_code => raw_params["serviceCode"],
        :central_service_code => raw_params["centralServiceCode"],
        :server_code => raw_params["serverCode"]
      }
    )
  end

  def render_details_visibility(privilege)
    render_json_without_messages({:can => can?(privilege)})
  end

  def render_cert_dump_and_hash(raw_cert)
    render_json({
      :cert_dump => CommonUi::CertUtils.cert_dump(raw_cert),
      :cert_hash => CommonUi::CertUtils.cert_hash(raw_cert)
    })
  end

  def disable_federation
    unless SystemProperties::getCenterTrustedAnchorsAllowed
      current_user.privileges -= FEDERATION_PRIVILEGES
    end
  end

  def read_locale
    return unless current_user

    ui_user = UiUser.find_by_username(current_user.name)
    I18n.locale = ui_user.locale if ui_user
  end

  def read_server_id
    return @server_id if @server_id
    @server_id = CentralServerId.new(SystemParameter.instance_identifier)
  end

  def read_ha_node_name
    return @ha_node_name if @ha_node_name
    @ha_node_name = CommonSql.ha_node_name
  end

  def validate_cert(uploaded_file)
    validate_any_cert(uploaded_file, CertValidator.new)
  end

  def validate_auth_cert(uploaded_file)
    validate_any_cert(uploaded_file, AuthCertValidator.new)
  end

  def validate_any_cert(uploaded_file, validator)
    CommonUi::UploadedFile::Validator.new(
        uploaded_file,
        validator).validate
  end

  class CentralServerId
    def initialize(xroad_instance)
      @xroad_instance = xroad_instance
    end

    # To make it compatible with common-ui
    def toShortString
      return @xroad_instance
    end
  end
end
