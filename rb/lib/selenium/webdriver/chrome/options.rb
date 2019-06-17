# frozen_string_literal: true

# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

module Selenium
  module WebDriver
    module Chrome
      class Options < WebDriver::Common::Options

        KEY = 'goog:chromeOptions'

        # see: http://chromedriver.chromium.org/capabilities
        CAPABILITIES = %i[args binary extensions local_state prefs detach debugger_address exclude_switches
                          minidump_path mobile_emulation perf_logging_prefs window_types].freeze

        (CAPABILITIES + %i[options emulation encoded_extensions]).each do |key|
          define_method key do
            @options[key]
          end

          define_method "#{key}=" do |value|
            @options[key] = value
          end
        end

        # Create a new Options instance.
        #
        # @example
        #   options = Selenium::WebDriver::Chrome::Options.new(args: ['start-maximized', 'user-data-dir=/tmp/temp_profile'])
        #   driver = Selenium::WebDriver.for(:chrome, options: options)
        #
        # @param [Hash] opts the pre-defined options to create the Chrome::Options with
        # @option opts [Array<String>] :args List of command-line arguments to use when starting Chrome
        # @option opts [String] :binary Path to the Chrome executable to use
        # @option opts [Hash] :prefs A hash with each entry consisting of the name of the preference and its value
        # @option opts [Array<String>] :extensions A list of paths to (.crx) Chrome extensions to install on startup
        # @option opts [Hash] :options A hash for raw options
        # @option opts [Hash] :emulation A hash for raw emulation options
        # @option opts [Hash] :local_state A hash for the Local State file in the user data folder
        # @option opts [Boolean] :detach whether browser is closed when the driver is sent the quit command
        # @option opts [String] :debugger_address address of a Chrome debugger server to connect to
        # @option opts [Array<String>] :exclude_switches command line switches to exclude
        # @option opts [String] :minidump_path Directory to store Chrome minidumps (linux only)
        # @option opts [Hash] :perf_logging_prefs A hash for performance logging preferences
        # @option opts [Array<String>] :window_types A list of window types to appear in the list of window handles
        #

        def initialize(emulation: nil, encoded_extensions: nil, options: nil, **opts)
          @options = if options
                       WebDriver.logger.deprecate(":options as keyword for initializing #{self.class}",
                                                  "values directly in #new constructor")
                       opts.merge(options)
                     else
                       opts
                     end
          @options[:mobile_emulation] ||= emulation if emulation
          @options[:encoded_extensions] = encoded_extensions if encoded_extensions
          @options[:extensions]&.each(&method(:validate_extension))
        end

        alias_method :emulation, :mobile_emulation
        alias_method :emulation=, :mobile_emulation=

        #
        # Add an extension by local path.
        #
        # @example
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_extension('/path/to/extension.crx')
        #
        # @param [String] path The local path to the .crx file
        #

        def add_extension(path)
          validate_extension(path)
          @options[:extensions] ||= []
          @options[:extensions] << path
        end

        #
        # Add an extension by Base64-encoded string.
        #
        # @example
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_encoded_extension(encoded_string)
        #
        # @param [String] encoded The Base64-encoded string of the .crx file
        #

        def add_encoded_extension(encoded)
          @options[:encoded_extensions] ||= []
          @options[:encoded_extensions] << encoded
        end

        #
        # Add a command-line argument to use when starting Chrome.
        #
        # @example Start Chrome maximized
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_argument('start-maximized')
        #
        # @param [String] arg The command-line argument to add
        #

        def add_argument(arg)
          @options[:args] ||= []
          @options[:args] << arg
        end

        #
        # Add a new option not yet handled by bindings.
        #
        # @example Leave Chrome open when chromedriver is killed
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_option(:detach, true)
        #
        # @param [String, Symbol] name Name of the option
        # @param [Boolean, String, Integer] value Value of the option
        #

        def add_option(name, value)
          @options[name] = value
        end

        #
        # Add a preference that is only applied to the user profile in use.
        #
        # @example Set the default homepage
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_preference('homepage', 'http://www.seleniumhq.com/')
        #
        # @param [String] name Key of the preference
        # @param [Boolean, String, Integer] value Value of the preference
        #

        def add_preference(name, value)
          @options[:prefs] ||= {}
          @options[:prefs][name] = value
        end

        #
        # Run Chrome in headless mode.
        #
        # @example Enable headless mode
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.headless!
        #

        def headless!
          add_argument '--headless'
        end

        #
        # Add an emulation device name
        #
        # @example Start Chrome in mobile emulation mode by device name
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_emulation(device_name: 'iPhone 6')
        #
        # @example Start Chrome in mobile emulation mode by device metrics
        #   options = Selenium::WebDriver::Chrome::Options.new
        #   options.add_emulation(device_metrics: {width: 400, height: 800, pixelRatio: 1, touch: true})
        #
        # @param [String] device_name Name of the device or a hash containing width, height, pixelRatio, touch
        # @param [Hash] device_metrics Hash containing width, height, pixelRatio, touch
        # @param [String] user_agent Full user agent
        #

        def add_emulation(**opt)
          @options[:mobile_emulation] = opt
        end

        #
        # @api private
        #

        def as_json(*)
          options = @options.dup

          opts = CAPABILITIES.each_with_object({}) do |capability_name, hash|
            capability_value = options.delete(capability_name)
            hash[capability_name] = capability_value unless capability_value.nil?
          end

          opts[:binary] ||= Chrome.path if Chrome.path
          extensions = opts[:extensions] || []
          opts[:extensions] = extensions.map(&method(:encode_extension)) +
                              (options.delete(:encoded_extensions) || [])
          opts.delete(:extensions) if opts[:extensions].empty?
          opts[:mobile_emulation] = process_emulation(opts[:mobile_emulation] || options.delete(:emulation) || {})
          opts.delete(:mobile_emulation) if opts[:mobile_emulation].empty?

          {KEY => generate_as_json(opts.merge(options))}
        end

        private

        def process_emulation(device_name: nil, device_metrics: nil, user_agent: nil)
          emulation = {}
          emulation[:device_name] = device_name if device_name
          emulation[:device_metrics] = device_metrics if device_metrics
          emulation[:user_agent] = user_agent if user_agent
          emulation
        end

        def encode_extension(path)
          File.open(path, 'rb') { |crx_file| Base64.strict_encode64 crx_file.read }
        end

        def validate_extension(path)
          raise Error::WebDriverError, "could not find extension at #{path.inspect}" unless File.file?(path)
          raise Error::WebDriverError, "file was not an extension #{path.inspect}" unless File.extname(path) == '.crx'
        end
      end # Options
    end # Chrome
  end # WebDriver
end # Selenium
