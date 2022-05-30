require 'asciidoctor'
require 'json'

if Gem::Version.new(Asciidoctor::VERSION) <= Gem::Version.new('1.5.1')
  fail 'asciidoctor: FAILED: HTML5/Slim backend needs Asciidoctor >=1.5.2!'
end

unless defined? Slim::Include
  fail 'asciidoctor: FAILED: HTML5/Slim backend needs Slim >= 2.1.0!'
end

# Add custom functions to this module that you want to use in your Slim
# templates. Within the template you can invoke them as top-level functions
# just like in Haml.
module Slim::Helpers
  def resolve_image_path(path, relative_to_attr = 'outdir')
    basedir = Pathname.new @document.attr(relative_to_attr)
    image = Pathname.new File.absolute_path(path, basedir.to_s)
    relative = image.relative_path_from basedir
    relative.to_s
  end
end