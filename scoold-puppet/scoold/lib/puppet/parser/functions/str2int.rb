module Puppet::Parser::Functions
  newfunction(:str2int, :type => :rvalue) do |args|
    Integer(args[0])
  end
end