#!/usr/bin/env perl6
use PKafka::Consumer;
use PKafka::Message;
use PKafka::Producer;
use HTTP::Client;

grammar Nginx
{
    rule TOP {^ .* '"' <host> <cmd> <path> <protocol> '"' <response> .* $}
    token host { \S+ }
    regex path { '/' <path-elem>? <path>?}
    token path-elem { \S+ }
    regex protocol { 'HTTP/1.1'}  
    token response { \d+ }

    proto token cmd {*}
    token cmd:sym<GET> { <sym> }
    token cmd:sym<POST> { <sym> }
};

grammar Hal
{
    rule TOP {<path> '?' <arg>+}
    rule path { '/' \w+ <path>?}
    token identifier {\w+}
    rule arg { <key=identifier> '=' <value=identifier> '&'?} 
};

class HalArgs
{
    method identifier($/) { $/.make: ~$/ }
    method arg($/) { $/.make: $<key>.made => $<value>.made}
    method TOP ($/) { $/.make: Map.new($<arg>».made)}
};

sub MAIN ($kafka-host, $henhouse-host) 
{
    my $config = PKafka::Config.new("group.id"=> "hal-test-1");
    my $rsyslog = PKafka::Consumer.new( topic=>"nginx", brokers=>$kafka-host, config=>$config);

    my $henhouse = IO::Socket::INET.new(:host($henhouse-host), :port<2003>);

    $rsyslog.messages.tap(-> $msg 
    {
        given $msg 
        {
            when PKafka::Message
            {
                my $r = Nginx.parse($msg.payload-str);
                say "MSG: {$msg.payload-str}";
                send-to-henhouse($r, $henhouse) if $r<cmd>;
                $rsyslog.save-offset($msg);
            }
            when PKafka::EOF
            {
                say "Messages Consumed { $msg.total-consumed}";
            }
            when PKafka::Error
            {
                say "Error {$msg.what}";
                $rsyslog.stop;
            }
        }
    });

    await $rsyslog.consume-from-last(partition=>0);
}

sub count($henhouse, Int $count, Str $key)
{
    my $stat = "$key $count {DateTime.now.posix}";
    say "HEN: $stat";

    #send to henhouse
    $henhouse.print("$stat\n");
}

sub track($henhouse, $path)
{
    my %args = Hal.parse($path, actions=>HalArgs).made;
    return if not %args<ch>:exists;

    my $channel = %args<ch>;
    my $subject = %args<sub>;
    my $verb = %args<v>;
    my $object = %args<ob>;
    my $object-id = %args<id>;
    my $count = val(%args<c>);

    return if $count.WHAT === Str;

    my $cluster = 1; #TODO, get these from tracking url
    my $context = 1; #TODO, get these from tracking url

    count($henhouse, $count, "track.$channel.$cluster.$context.$object.$object-id.$verb.$subject");
    count($henhouse, $count, "track.$channel.$cluster.$context.$object.$object-id.$verb");
    count($henhouse, $count, "track.$channel.$cluster.$context.$object.$verb");
    count($henhouse, $count, "track.$channel.$cluster.$object.$object-id.$verb.$subject");
    count($henhouse, $count, "track.$channel.$cluster.$object.$object-id.$verb");
    count($henhouse, $count, "track.$channel.$cluster.$object.$verb");
    count($henhouse, $count, "track.$channel.$object.$object-id.$verb.$subject");
    count($henhouse, $count, "track.$channel.$object.$object-id.$verb");
    count($henhouse, $count, "track.$channel.$object.$verb");
    count($henhouse, $count, "track.$object.$object-id.$verb.$subject");
    count($henhouse, $count, "track.$object.$object-id.$verb");
    count($henhouse, $count, "track.$object.$verb");
    count($henhouse, $count, "track.$verb.$subject");
    count($henhouse, $count, "track.$verb");
}

sub send-to-henhouse($r, $henhouse)
{
    #only count things river-rock proxies
    return if not $r<host> ~~ m/river\-rock/;

    if $r<path> ~~ m/api\/v1\/hal/ {
        track($henhouse, $r<path>) 
    } 
    else 
    {
        count($henhouse, "$r<path>.$r<cmd>.$r<response>");
    }
}

